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

    private static void makeRecorder(boolean createNew, int source, int outputFormat, int audioEncoder) {
        if(createNew)
            recorder = new MediaRecorder();
        else
            recorder.reset();

        recorder.setAudioSource(source);
        recorder.setOutputFormat(outputFormat);
        recorder.setAudioEncoder(audioEncoder);
        recorder.setOutputFile(audioFile.getAbsolutePath());
    }

    static void doRecording(Context context, String phoneNumber, String callIdentifier) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CallRecorderApplication.getInstance());
        String speakerUse = settings.getString(SettingsFragment.SPEAKER_USE, "always_off");
        String recordingFormat = settings.getString(SettingsFragment.FORMAT, "amr_nb");
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.
                getSystemService(Context.NOTIFICATION_SERVICE);
        String fileExtension = recordingFormat.equals("aac") || recordingFormat.equals("he_aac") ? ".3gp" : ".amr";
        int outputFormat, audioEncoder;

        if(recorder != null)
            return ;
        audioFile = new File(context.getFilesDir(), phoneNumber + System.currentTimeMillis() + fileExtension);
        try {
            audioFile.createNewFile();
        } catch (IOException ioe) {
            Log.wtf(TAG, "Error creating the audio file: " + ioe.getMessage());
        }

        switch (recordingFormat) {
            case "aac": outputFormat = MediaRecorder.OutputFormat.THREE_GPP;
                audioEncoder = MediaRecorder.AudioEncoder.AAC;
                break;
            case "he_aac": outputFormat = MediaRecorder.OutputFormat.THREE_GPP;
                audioEncoder = MediaRecorder.AudioEncoder.HE_AAC;
                break;
            case "amr_nb": outputFormat = MediaRecorder.OutputFormat.AMR_NB;
                audioEncoder = MediaRecorder.AudioEncoder.AMR_NB;
                break;
            case "amr_wb": outputFormat = MediaRecorder.OutputFormat.AMR_WB;
                audioEncoder = MediaRecorder.OutputFormat.AMR_WB;
                break;
            default: outputFormat = MediaRecorder.OutputFormat.AMR_NB;
                audioEncoder = MediaRecorder.AudioEncoder.AMR_NB;
        }

        makeRecorder(true, MediaRecorder.AudioSource.VOICE_CALL, outputFormat, audioEncoder);
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e1) {
            Log.wtf(TAG, "VOICE_CALL exception: " + e1.getClass() + ": " + e1.getMessage());
            makeRecorder(false, MediaRecorder.AudioSource.VOICE_RECOGNITION, outputFormat, audioEncoder);

            try {
                recorder.prepare();
                recorder.start();
            } catch (Exception e2) {
                Log.wtf(TAG, "VOICE_RECOGNITION exception: " + e2.getClass() + ": " + e2.getMessage());
                makeRecorder(false, MediaRecorder.AudioSource.VOICE_COMMUNICATION, outputFormat, audioEncoder);

                try {
                    recorder.prepare();
                    recorder.start();
                } catch (Exception e3) {
                    Log.wtf(TAG, "VOICE_COMMUNICATION exception: " + e3.getClass() + ": " + e3.getMessage());
                    makeRecorder(false, MediaRecorder.AudioSource.MIC, outputFormat, audioEncoder);

                    try {
                        recorder.prepare();
                        recorder.start();
                    }
                    catch (Exception e4) {
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
