package net.synapticweb.callrecorder.recorder;


import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

class RecorderBox {
    private static final String TAG = "CallRecorder";
    private static MediaRecorder recorder = null;
    private static File audioFile;
    private static long startTimestamp;
    private static boolean recordingDone = false;

    static final String ACTION_STOP_RECORDING = "net.synapticweb.callrecorder.STOP_RECORDING";
    static final String ACTION_START_RECORDING = "net.synapticweb.callrecorder.START_RECORDING";
    static final String ACTION_PAUSE_RECORDING = "net.synapticweb.callrecorder.PAUSE_RECORDING";
    static final String ACTION_RESUME_RECORDING = "net.synapticweb.callrecorder.RESUME_RECORDING";

    private RecorderBox(){}

    static void doRecording(Context context, String phoneNumber)
    {
        if(recorder != null)
            return ;
        audioFile = new File(context.getFilesDir(), phoneNumber + System.currentTimeMillis() + ".amr");
        try {
            audioFile.createNewFile();
        } catch (IOException ioe) {
            Log.wtf(TAG, "Error creating the audio file: " + ioe.getMessage());
        }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        recorder.setOutputFile(audioFile.getAbsolutePath());

        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            Log.wtf(TAG, "VOICE_CALL exception: " + e.getClass() + ": " + e.getMessage());

            recorder.reset();
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            recorder.setOutputFile(audioFile.getAbsolutePath());

            try {
                recorder.prepare();
                recorder.start();
            } catch (Exception e2)
            {
                Log.wtf(TAG, "VOICE_COMMUNICATION exception: " + e2.getClass() + ": " + e2.getMessage());
                recorder.reset();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile(audioFile.getAbsolutePath());

                try {
                    recorder.prepare();
                    recorder.start();
                } catch (Exception e3)
                {
                    Log.wtf(TAG, "MIC exception: " + e3.getClass() + ": " + e3.getMessage());
                    return ;
                }
            }
        }
        startTimestamp = System.currentTimeMillis();
        recordingDone = true;
    }

    static void disposeRecorder()
    {
        if(recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    static void pauseRecorder()
    {
        recorder.pause();
    }

    @TargetApi(Build.VERSION_CODES.N)
    static void resumeRecorder()
    {
        recorder.resume();
    }

    static long getStartTimestamp()
    {
        return startTimestamp;
    }

    static String getAudioFilePath()
    {
        return audioFile.getAbsolutePath();
    }

    static boolean getRecordingDone(){
        return recordingDone;
    }
}
