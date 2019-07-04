package net.synapticweb.callrecorder.recorder;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

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
