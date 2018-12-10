package net.synapticweb.callrecorder.recorder;


import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import net.synapticweb.callrecorder.CallRecorderApplication;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.settings.SettingsFragment;

import java.io.File;
import java.io.IOException;

class RecorderBox {
    private static final String TAG = "CallRecorder";
    private static MediaRecorder recorder = null;
    private static AudioManager audioManager = null;
    private static File audioFile = null;
    private static Long startTimestamp = null;
    private static boolean recordingDone = false;

    static final String ACTION_START_RECORDING = "net.synapticweb.callrecorder.START_RECORDING";
    static final String ACTION_STOP_SPEAKER = "net.synapticweb.callrecorder.STOP_SPEAKER";

    private RecorderBox(){}

    private static void makeRecorder(boolean createNew, int source) {
        if(createNew)
            recorder = new MediaRecorder();
        else
            recorder.reset();
        recorder.setAudioSource(source);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        recorder.setOutputFile(audioFile.getAbsolutePath());
    }

    static void doRecording(Context context, String phoneNumber, String callIdentifier) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CallRecorderApplication.getInstance());
        String speakerUse = settings.getString(SettingsFragment.SPEAKER_USE, "");
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.
                getSystemService(Context.NOTIFICATION_SERVICE);

        if(recorder != null)
            return ;
        audioFile = new File(context.getFilesDir(), phoneNumber + System.currentTimeMillis() + ".amr");
        try {
            audioFile.createNewFile();
        } catch (IOException ioe) {
            Log.wtf(TAG, "Error creating the audio file: " + ioe.getMessage());
        }

        makeRecorder(true, MediaRecorder.AudioSource.MIC);
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            Log.wtf(TAG, "VOICE_CALL exception: " + e.getClass() + ": " + e.getMessage());
            makeRecorder(false, MediaRecorder.AudioSource.VOICE_COMMUNICATION);

            try {
                recorder.prepare();
                recorder.start();
            } catch (Exception e2)
            {
                Log.wtf(TAG, "VOICE_COMMUNICATION exception: " + e2.getClass() + ": " + e2.getMessage());
                makeRecorder(false, MediaRecorder.AudioSource.MIC);

                try {
                    recorder.prepare();
                    recorder.start();
                } catch (Exception e3)
                {
                    Log.wtf(TAG, "MIC exception: " + e3.getClass() + ": " + e3.getMessage());
                    return ;
                }
                if(!speakerUse.equals(context.getResources().getStringArray(R.array.speaker_options_values)[2])) { //always_off
                   putSpeakerOn();
                    if(notificationManager != null)
                        notificationManager.notify(RecorderService.NOTIFICATION_ID,
                                RecorderService.buildNotification(RecorderService.RECORD_AUTOMMATICALLY_SPEAKER_ON, callIdentifier));
                }
            }
        }
        startTimestamp = System.currentTimeMillis();
        recordingDone = true;
        if(speakerUse.equals(context.getResources().getStringArray(R.array.speaker_options_values)[1])) { //always_on
           putSpeakerOn();
            if(notificationManager != null)
                notificationManager.notify(RecorderService.NOTIFICATION_ID,
                        RecorderService.buildNotification(RecorderService.RECORD_AUTOMMATICALLY_SPEAKER_ON, callIdentifier));
        }
    }

    private static void putSpeakerOn() {
        if(audioManager != null && !audioManager.isSpeakerphoneOn()) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
            Log.wtf(TAG, "Volume before adjust: " + audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
            Log.wtf(TAG, "Volume after adjust: " + audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
            Log.wtf(TAG, "Speaker is on: " + audioManager.isSpeakerphoneOn());
        }
    }

    static void resetState() {
        audioManager = null;
        audioFile = null;
        startTimestamp = null;
        recordingDone = false;
    }

    static void disposeRecorder() {
        if(recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
        if(audioManager != null && audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
            Log.wtf(TAG, "Speaker is on: " + audioManager.isSpeakerphoneOn());
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    static void pauseRecorder() {
        recorder.pause();
    }

    @TargetApi(Build.VERSION_CODES.N)
    static void resumeRecorder() {
        recorder.resume();
    }

    static long getStartTimestamp() {
        return startTimestamp;
    }

    static String getAudioFilePath() {
        return audioFile.getAbsolutePath();
    }

    static boolean getRecordingDone(){
        return recordingDone;
    }
}
