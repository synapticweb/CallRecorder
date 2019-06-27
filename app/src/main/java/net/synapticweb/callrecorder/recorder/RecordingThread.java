package net.synapticweb.callrecorder.recorder;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.preference.PreferenceManager;
import android.util.Log;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.settings.SettingsFragment;


abstract class RecordingThread {
    protected static final String TAG = "CallRecorder";
    static final int SAMPLE_RATE = 44100;
    final int channels;
    final int bufferSize;
    final AudioRecord audioRecord;

    RecordingThread(String mode) {
        channels = (mode.equals(Recorder.MONO) ? 1 : 2);
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = createAudioRecord();
        audioRecord.startRecording();
    }

    //https://stackoverflow.com/questions/26088427/increase-volume-output-of-recorded-audio
    //Alte topicuri relevante:
    //https://stackoverflow.com/questions/14485873/audio-change-volume-of-samples-in-byte-array
    //https://stackoverflow.com/questions/4300995/modify-volume-gain-on-audio-sample-buffer
    //https://github.com/JorenSix/TarsosDSP
    //https://stackoverflow.com/questions/10578865/android-audiorecord-apply-gain-with-variation
    //https://stackoverflow.com/questions/25441166/how-to-adjust-microphone-sensitivity-while-recording-audio-in-android
    //https://stackoverflow.com/questions/26317772/increase-volume-of-recording-android-audiorecord
     void addGain(byte[] audioData) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
        int gainDb = Integer.valueOf(settings.getString(SettingsFragment.GAIN, "0"));
        if(gainDb == 0)
            return ;

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

    private AudioRecord createAudioRecord() {
        AudioRecord audioRecord;
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_CALL, SAMPLE_RATE,
                    channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
            if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                throw new Exception("VOICE_CALL source unavailable");
        }
        catch (Exception e1) {
            Log.wtf(TAG, e1.getMessage());
            try {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                        channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                    throw new Exception("Voice recognition source unavailable");
            } catch (Exception e2) {
                Log.wtf(TAG, e2.getMessage());
                try {
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                            channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
                    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                        throw new Exception("VOICE_COMMUNICATION source unavailable");
                } catch (Exception e3) {
                    Log.wtf(TAG, e3.getMessage());
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                            channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
                    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                        throw new RuntimeException("Unable to initialize AudioRecord");
                }
            }
        }

        if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
            android.media.audiofx.NoiseSuppressor noiseSuppressor = android.media.audiofx.NoiseSuppressor
                    .create(audioRecord.getAudioSessionId());
            if (noiseSuppressor != null) {
                noiseSuppressor.setEnabled(true);
            }
        }

        if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
            android.media.audiofx.AutomaticGainControl automaticGainControl = android.media.audiofx.AutomaticGainControl
                    .create(audioRecord.getAudioSessionId());
            if (automaticGainControl != null) {
                automaticGainControl.setEnabled(true);
            }
        }

        return audioRecord;
    }

    void disposeAudioRecord() {
        audioRecord.stop();
        audioRecord.release();
    }
}
