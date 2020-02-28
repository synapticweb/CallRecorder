/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import androidx.annotation.NonNull;

import net.synapticweb.callrecorder.BuildConfig;
import net.synapticweb.callrecorder.CrLog;

import org.acra.ACRA;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


//Playerul a fost gîndit pe baza arhitecturii de aici:
//https://medium.com/google-developers/building-a-simple-audio-app-in-android-part-2-3-a514f6224b83
@SuppressWarnings("CatchMayIgnoreException")
class AudioPlayer extends Thread implements PlayerAdapter {
    private int state;
    private String mediaPath;
    private PlaybackListenerInterface playbackListener;
    private ScheduledExecutorService executor;
    private Runnable seekbarPositionUpdateTask;
    private static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 500;
    private MediaCodec decoder;
    private MediaExtractor extractor;
    private AudioTrack audioTrack;
    private boolean stop = false;
    private boolean paused = false;
    private static final int SAMPLE_RATE = 44100;
    private String formatName;
    private static final String WAV_FORMAT = "wav";
    private static final String AAC_FORMAT = "aac";
    private int channelCount;
    private RandomAccessFile inputWav; //folosesc RandomAccessFile deoarece, spre deosebire de InputStream,
    //permite operațiuni seek
    private long wavBufferCount = 0;
    private static final int WAV_BUFFER_SIZE = 8092;
    private int maxWavBuffers;
    private float gainDb = 0;

    private final static String ACRA_MODE = "mode";
    private final static String ACRA_SIZE = "file_size";
    private final static String ACRA_FORMAT = "format";


    AudioPlayer(PlaybackListenerInterface listener) {
        this.playbackListener = listener;
        state = PlayerAdapter.State.UNINITIALIZED;
    }

    @Override
    public void setGain(float gain) {
        gainDb = gain;
    }

    //https://stackoverflow.com/questions/26088427/increase-volume-output-of-recorded-audio
    //Alte topicuri relevante:
    //https://stackoverflow.com/questions/14485873/audio-change-volume-of-samples-in-byte-array
    //https://stackoverflow.com/questions/4300995/modify-volume-gain-on-audio-sample-buffer
    //https://github.com/JorenSix/TarsosDSP
    //https://stackoverflow.com/questions/10578865/android-audiorecord-apply-gain-with-variation
    //https://stackoverflow.com/questions/25441166/how-to-adjust-microphone-sensitivity-while-recording-audio-in-android
    //https://stackoverflow.com/questions/26317772/increase-volume-of-recording-android-audiorecord
    private void addGain(@NonNull byte[] audioData) {
        double gainFactor = Math.pow(10, gainDb / 20);
        for (int i = 0; i < audioData.length; i += 2) {
            float sample = (float) (audioData[i] & 0xff | audioData[i + 1] << 8);
            sample *= gainFactor;

            if (sample >= 32767f) {
                audioData[i] = (byte) 0xff;
                audioData[i + 1] = 0x7f;
            }
            else if ( sample <= -32768f ) {
                audioData[i] = 0x0;
                audioData[i + 1] = (byte) 0x80;
            }
            else {
                int s = (int) (0.5f + sample);
                audioData[i] = (byte)(s & 0xFF);
                audioData[i + 1] = (byte)(s >> 8 & 0xFF);
            }
        }
    }

    @Override
    public boolean setMediaPosition(int position) {
        //nu văd probleme cu apelul în INITIALIZED. Există use-case pentru apelul în PLAYING sau în PAUSE: întoarcerea ecranului.
        if(state == PlayerAdapter.State.UNINITIALIZED)
            CrLog.log(CrLog.WARN, "Invoked setMediaPosition() while in UNINITIALIZED state");

        if(seekTo(position)) {
            playbackListener.onPositionChanged(position);
            return true;
        }
        return false;
    }

    //În cazul WAV:
    //poziția care trebuie raportată este milisecunda la care ne aflăm. Ca să obținem asta trebuie să știm
    //cîți octeți au fost redați și nr octeților redați este aflat numărînd bufferele redate. Împărțind această
    //cifră la nr de octeți / secundă obținem secunda la care ne aflăm. (Folosim împărțirea cu double ca să evităm
    //o rotunjire în jos la împărțirea de întregi care produce un bug: după ce mutăm cursorul la un timp nou
    //cursorul se duce puțin în jos - pentru că poziția este raportată incorectă ca fiind mai mică decît
    //ce reală.) Apoi înmulțim cu o mie ca să obținem poziția în milisecunde și convertim la int.
    //Valoarea maximă a unui int: 2147483647. Suficient pentru ~ 596 de ore.
    @Override
    public int getCurrentPosition() {
        //nu văd probleme cu apelul în INITIALIZED
        if(state == PlayerAdapter.State.UNINITIALIZED)
           CrLog.log(CrLog.WARN, "Attempt to invoke getCurrentPosition while in UNINITIALIZED state");
        if(formatName.equals(AAC_FORMAT))
            return (int) extractor.getSampleTime() / 1000;

        long bytesRead = WAV_BUFFER_SIZE * wavBufferCount;
        int bytesBySecond = SAMPLE_RATE * channelCount * 2;

        return (int) Math.ceil((double) bytesRead / bytesBySecond) * 1000;
    }

    //https://stackoverflow.com/questions/21861220/audiotrack-seek-in-android
    @Override
    public boolean seekTo(int position) {
        //nu văd probleme cu apelul în INITIALIZED
        if(state == PlayerAdapter.State.UNINITIALIZED)
            CrLog.log(CrLog.WARN, "Attempt to invoke seekTo() while in UNINITIALIZED state");
        if(formatName.equals(AAC_FORMAT))
            extractor.seekTo((long) position * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC); //MediaExtractor folosește microsecunde, nu milisecunde
        else {
            //44100 samples * 2 channels * 2 bytes per sample = number of bytes per second;
            //number of bytes per second / 1000 = number of bytes per millisecond;
            //number of bytes per millisecond * position = position in milliseconds
            long newposition = ((SAMPLE_RATE * channelCount * 2) / 1000) * position;
            try {
                inputWav.seek(newposition);
            }
            catch (IOException e) {
                CrLog.log(CrLog.ERROR, "Error while seeking: " + e.getMessage() + " Aborting...");
                playbackListener.onError();
                if(isAlive())
                    interrupt();
                return false;
            }
            wavBufferCount = newposition / WAV_BUFFER_SIZE; //dacă nu updatăm wavBufferCount cursorul va sări înapoi imediat
        }
        return true;
    }

    @Override
    public boolean loadMedia(String mediaPath) {
        //se apelează în mod obișnuit după construcție, în UNINITIALIZED. Apelarea în INITIALIZED deși inutilă
        //este inofensivă. Apelul în PLAYING sau PAUSE poate crea probleme, de aceea am interzis.
        if(state == PlayerAdapter.State.PLAYING || state == PlayerAdapter.State.PAUSED)
            CrLog.log(CrLog.WARN, "Attempt to invoke loadMedia() while in PLAYING or PAUSED state");
        this.mediaPath = mediaPath;
        try {
            initialize();
        }
        catch (PlayerException e) {
            CrLog.log(CrLog.ERROR, "Error while initializing: " + e.getMessage() + " Aborting...");
            playbackListener.onError();
            return false;
        }

        playbackListener.onDurationChanged(getTotalDuration());
        playbackListener.onPositionChanged(0);
        return true;
    }

   private void initialize() throws PlayerException {
        formatName = mediaPath.endsWith(".wav") ? WAV_FORMAT : AAC_FORMAT;
        if(formatName.equals(AAC_FORMAT))
            initializeForAac(mediaPath); //exception
        else
            initializeForWav(mediaPath); //exception

        int channelConfig = channelCount == 1 ? AudioFormat.CHANNEL_OUT_MONO :
                AudioFormat.CHANNEL_OUT_STEREO;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                channelConfig, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(SAMPLE_RATE,
                channelConfig, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        audioTrack.play();
        File audioFile = new File(mediaPath);

       //noinspection CatchMayIgnoreException
       try {
           ACRA.getErrorReporter().putCustomData(ACRA_FORMAT, formatName);
           ACRA.getErrorReporter().putCustomData(ACRA_MODE, channelCount == 1 ? "mono" : "stereo");
           ACRA.getErrorReporter().putCustomData(ACRA_SIZE, (audioFile.length() / 1024) + "KB");
       }
       catch (IllegalStateException exc) {
       }
        state = PlayerAdapter.State.INITIALIZED;
    }

    //http://soundfile.sapp.org/doc/WaveFormat/
    //https://thiscouldbebetter.wordpress.com/2011/08/14/reading-and-writing-a-wav-file-in-java/
    //https://stackoverflow.com/questions/3925030/using-audiotrack-in-android-to-play-a-wav-file
    //https://gist.github.com/muetzenflo/3e83975aba6abe63413abd98bb33c401
    private void initializeForWav(String mediaPath) throws PlayerException {
        final int WAV_HEADER_SIZE = 44;
        final int DATA_SIZE_ADDRESS = 40; //adresa de la care se citește mărimea secțiunii "data" a fișierului
        final int CHANNEL_COUNT_ADDRESS = 22; //adresa de la care se citește nr de canale
        int dataSize;
        byte[] dataSizeBytes = new byte[4];

        try {
            inputWav = new RandomAccessFile(mediaPath, "r");
            inputWav.seek(DATA_SIZE_ADDRESS);
            inputWav.read(dataSizeBytes);
            inputWav.seek(CHANNEL_COUNT_ADDRESS);
            channelCount = (int) inputWav.readByte();
            inputWav.seek(0);
            if(inputWav.skipBytes(WAV_HEADER_SIZE) < WAV_HEADER_SIZE)
                throw new PlayerException("Initialization error: Wav file corrupted");
        }
        catch (Exception e) {
            throw new PlayerException("Initialization error: " + e.getMessage());
        }

        //Acest nr este stocat în headerul wav în format little endian, de aceea nu îl pot citi cu
        //RandomAccessFile.readInt() care vrea big endian. E musai să citesc acest nr deoarece, cel puțin
        //în wav-urile produse de această aplicație (trebuie să aflu de ce) datele audio propriu-zise se
        //termină înainte de finalul fișierului (în spațiul rămas sunt copiate niște sample-uri) ca umplutură pînă
        //se face o secțiune "data" cu dimensiune putere de 2! Odată aflată mărimea segmentului valid
        //împart la buffersize pentru a afla nr maxim de bufferuri care trebuie citite pentru a nu
        //trece dincolo de segmentul valid. Masca 0xff e necesară pentru că octeții care au cel mai
        //semnificativ bit 1 sunt transformați în int copiind cel mai semnificativ bit, rezultînd astfel
        //în mod greșit un nr negativ.
        dataSize = ((int) dataSizeBytes[3] & 0xff) << 24 | ((int) dataSizeBytes[2] & 0xff) << 16 |
                ((int) dataSizeBytes[1] & 0xff) << 8 | (int) dataSizeBytes[0] & 0xff;
        maxWavBuffers = (int) Math.ceil((double) dataSize / WAV_BUFFER_SIZE);
    }

    private void initializeForAac(String mediaPath) throws PlayerException {
        MediaFormat format;
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(mediaPath);
            format = extractor.getTrackFormat(0); //Există situații cînd mărimea fișierului e 0 și acest
            // apel produce InvalidArgumentException
            decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            decoder.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            extractor.selectTrack(0);
            decoder.start();
        }
        catch (Exception e) {
            throw new PlayerException("Initialization error: " + e.getMessage());
        }
    }

    //poate fi apelat oricînd
    @Override
    public void stopPlayer() {
        state = PlayerAdapter.State.STOPPED;
        stop = true;
        resumeIfPaused();
    }

    @Override
    public void setPlayerState(int state) {
        this.state = state;
    }

    @Override
    //nu trebuie apelat în UNINITIALIZED din motive evidente. În INITIALIZED sau în PAUSED este apelarea
    //normală; în PLAYING nu are niciun efect.
    public void play() {
        if(state == PlayerAdapter.State.UNINITIALIZED)
            CrLog.log(CrLog.WARN, "Attempt to invoke play while in UNINITIALIZED state");
        if(!isAlive())
            start();
        else
            resumeIfPaused();
        startUpdatingPosition();
        state = PlayerAdapter.State.PLAYING;
    }

    //poate fi apelat oricînd. Produce un nou player cu state = UNINITIALIZED.
    @Override
    public void reset() {
        stopPlayer();
        playbackListener.onReset();
    }

    @Override
    public void pause() {
        //pause() nu trebuie chemat în stările UNINITIALIZED sau INITIALIZED pentru că va seta în mod greșit
        //și inutil flagul pause. Apelarea în timpul PAUSED nu are niciun efect.
        if(state == PlayerAdapter.State.UNINITIALIZED || state == PlayerAdapter.State.INITIALIZED)
            CrLog.log(CrLog.WARN, "Attempt to invoke pause() while in UNINITIALIZED state or INITIALIZED state");
        pauseIfRunning();
        stopUpdatingPosition(false);
        state = PlayerAdapter.State.PAUSED;
    }


    @Override
    public int getPlayerState() {
        return state;
    }

    @Override
    public int getTotalDuration() { //in milliseconds
        //doar UNINITIALIZED e interzis
        if(state == PlayerAdapter.State.UNINITIALIZED)
            CrLog.log(CrLog.WARN, "Attempt to invoke getTotalDuration() while in UNINITIALIZED state");
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mediaPath);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Integer.parseInt(time);
    }

    private void startUpdatingPosition() {
        if(executor == null)
            executor = Executors.newSingleThreadScheduledExecutor();
        if(seekbarPositionUpdateTask == null)
            seekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    if(state == PlayerAdapter.State.PLAYING) {
                        int currentPosition = getCurrentPosition();
                        if(playbackListener != null)
                            playbackListener.onPositionChanged(currentPosition);
                    }
                }
            };
        executor.scheduleAtFixedRate(seekbarPositionUpdateTask, 0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    private void stopUpdatingPosition(boolean resetPosition) {
        if(executor != null) {
            executor.shutdownNow();
            executor = null;
            seekbarPositionUpdateTask = null;
            if(resetPosition && playbackListener != null)
                playbackListener.onPositionChanged(0);
        }
    }

    private synchronized void resumeIfPaused() {
        //https://docs.oracle.com/javase/8/docs%2Ftechnotes%2Fguides%2Fconcurrency%2FthreadPrimitiveDeprecation.html
        if(paused){
            paused = false;
            notify();
        }
    }

    private synchronized void pauseIfRunning() {
        if(!paused)
            paused = true;
    }


    //https://dpsm.wordpress.com/2012/07/28/android-mediacodec-decoded/
    //https://sohailaziz05.blogspot.com/2014/06/mediacodec-decoding-aac-android.html -- de aici am adaptat
    //https://stackoverflow.com/questions/28701102/how-to-stream-data-from-mediacodec-to-audiotrack-with-xamarin-for-android -- asta nu prea se potrivește :(
    //TODO: error handling see docs. La momentul actual erorile de inițializare produc InitializationErrorException
    // care este prinsă în loadMedia() și este afișat un mesaj de eroare iar butoanele sunt dezactivate. În timpul
    // playbackului pot să apară CodecExceptions care sunt unchecked - produc oprirea aplicației. Poate ar trebui gestionate?
    private void playAac() throws PlayerException {
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] decoderOutputBuffers = decoder.getOutputBuffers();
        MediaCodec.BufferInfo bufInfo = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        final int timeOutUs = 10000;
        int inputBufId;
        int outputBufId;

        while(!sawOutputEOS && !stop) {
            //https://docs.oracle.com/javase/8/docs%2Ftechnotes%2Fguides%2Fconcurrency%2FthreadPrimitiveDeprecation.html
            if(paused) {
                try {
                    synchronized(this) {
                        while(paused)
                            wait();
                    }
                }
                catch (InterruptedException exc) {
                    CrLog.log(CrLog.WARN, "Synchronization error: " + exc.getMessage()); //trebuie gestionată eroarea?
                }
            }

            if(!sawInputEOS){
                inputBufId = decoder.dequeueInputBuffer(timeOutUs);
                if (inputBufId >= 0) {
                    ByteBuffer inputBuffer;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        inputBuffer = decoder.getInputBuffer(inputBufId);
                    else
                        inputBuffer = decoderInputBuffers[inputBufId];
                    if(inputBuffer == null)
                        throw new PlayerException("Codec returned null input buffer");
                    int sampleSize = extractor.readSampleData(inputBuffer, 0 /* offset */);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else
                        presentationTimeUs = extractor.getSampleTime();

                    decoder.queueInputBuffer(inputBufId, 0 /* offset */,
                            sampleSize, presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    : 0);
                    if (!sawInputEOS)
                        extractor.advance();
                }
            }
            outputBufId = decoder.dequeueOutputBuffer(bufInfo, timeOutUs);
            if(outputBufId >= 0) {
                ByteBuffer outputBuffer;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    outputBuffer = decoder.getOutputBuffer(outputBufId);
                else
                    outputBuffer = decoderOutputBuffers[outputBufId];
                if(outputBuffer == null)
                    throw new PlayerException("Codec returned null output buffer.");
                byte[] audioData = new byte[bufInfo.size];
                outputBuffer.get(audioData);
                outputBuffer.clear();

                if (audioData.length > 0) {
                    if(gainDb > 0)
                        addGain(audioData);
                    audioTrack.write(audioData, 0, audioData.length); //play
                }

                decoder.releaseOutputBuffer(outputBufId, false);
                if ((bufInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                    sawOutputEOS = true;
            }
            else if(outputBufId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
                decoderOutputBuffers = decoder.getOutputBuffers();
        }

        if(sawOutputEOS)
            playbackListener.onPlaybackCompleted();

        decoder.stop();
        decoder.release();
        extractor.release();
        audioTrack.stop();
        audioTrack.release();
    }

    private void playWav() throws PlayerException {
        int bytesRead;
        byte[] audioBuffer = new byte[WAV_BUFFER_SIZE];
        while(wavBufferCount <= maxWavBuffers && !stop) {
            if(paused) {
                try {
                    synchronized (this) {
                        while (paused)
                            wait();
                    }
                }
                catch (InterruptedException e) {
                    CrLog.log(CrLog.WARN, "Synchronization error: " + e.getMessage());
                }
            }
            try {
                bytesRead = inputWav.read(audioBuffer);
            }
            catch (IOException e) {
                throw new PlayerException("Error reading from the wav file");
            }
            ++wavBufferCount;

            if(gainDb > 0)
                addGain(audioBuffer);
            audioTrack.write(audioBuffer, 0, bytesRead);
        }

        if(wavBufferCount >= maxWavBuffers)
            playbackListener.onPlaybackCompleted();

        audioTrack.stop();
        audioTrack.release();
    }

    @Override
    public void run() {
        try {
            if (formatName.equals(AAC_FORMAT))
                playAac();
            else
                playWav();
        }
        catch (PlayerException e) {
            playbackListener.onError();
        }
        stopUpdatingPosition(true);
       try {
           ACRA.getErrorReporter().clearCustomData();
       }
       catch (IllegalStateException ignored) {}
    }

}