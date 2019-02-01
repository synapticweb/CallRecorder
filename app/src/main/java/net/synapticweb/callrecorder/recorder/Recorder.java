package net.synapticweb.callrecorder.recorder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import net.synapticweb.callrecorder.CallRecorderApplication;
import net.synapticweb.callrecorder.settings.SettingsFragment;

import java.io.File;

class Recorder {
    private static final String TAG = "CallRecorder";
    private File audioFile;
    private Thread recordingThread;
    private long startingTime;
    private final String format;
    private final String mode;
    private static final String WAV_FORMAT = "wav";
    static final String AAC_HIGH_FORMAT = "aac_hi";
    static final String AAC_MEDIUM_FORMAT = "aac_med";
    static final String MONO = "mono";

     Recorder() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CallRecorderApplication.getInstance());
        format = settings.getString(SettingsFragment.FORMAT, "");
        mode = settings.getString(SettingsFragment.MODE, "");
    }


    long getStartingTime() {
        return startingTime;
    }

    String getAudioFilePath() {
        return audioFile.getAbsolutePath();
    }

    void startRecording(String phoneNumber) {
        if(phoneNumber == null)
            throw new NullPointerException("Recorder.startRecoring(): phoneNumber cannot be null");

        if(recordingThread != null)
            stopRecording();
        String extension = format.equals(WAV_FORMAT) ? ".wav" : ".aac";

        audioFile = new File(CallRecorderApplication.getInstance().getFilesDir(), phoneNumber + System.currentTimeMillis() + extension);
        try {
            if(format.equals(WAV_FORMAT))
                recordingThread = new Thread(new RecordingThreadWav(mode));
            else
                recordingThread = new Thread(new RecordingThreadAac(audioFile, format, mode));
        }
        catch(RuntimeException e) {
            Log.wtf(TAG, e.getMessage());
            return ;
        }
        recordingThread.start();
        startingTime = System.currentTimeMillis();
    }

    void stopRecording() {
        if(recordingThread != null) {
                recordingThread.interrupt();
            recordingThread = null;
            if(format.equals(WAV_FORMAT)) {
                Thread copyPcmToWav = new Thread(new RecordingThreadWav.CopyPcmToWav(audioFile, mode));
                copyPcmToWav.start();
            }
        }
    }

    boolean isRunning() {
        return recordingThread != null;
    }

    public String getFormat() {
        return format;
    }

    public String getMode() {
        return mode;
    }

}
