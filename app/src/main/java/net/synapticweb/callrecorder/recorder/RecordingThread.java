/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.recorder;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.preference.PreferenceManager;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.settings.SettingsFragment;


import static android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION;

abstract class RecordingThread {
    static final int SAMPLE_RATE = 44100;
    final int channels;
    final int bufferSize;
    final AudioRecord audioRecord;
    protected final Recorder recorder;
    protected Context context;

    RecordingThread(Context context, String mode, Recorder recorder) throws RecordingException {
        this.context = context;
        channels = (mode.equals(Recorder.MONO) ? 1 : 2);
        this.recorder = recorder;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = createAudioRecord();
        audioRecord.startRecording();
    }

    private AudioRecord createAudioRecord() throws RecordingException {
        AudioRecord audioRecord;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int source = Integer.valueOf(settings.getString(SettingsFragment.SOURCE,
                String.valueOf(VOICE_RECOGNITION)));
            try {
                audioRecord = new AudioRecord(source, SAMPLE_RATE,
                        channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
            } catch (Exception e) { //La VOICE_CALL dă IllegalArgumentException. Aplicația nu se oprește, rămîne
                //hanging, nu înregistrează nimic.
                throw new RecordingException(e.getMessage());
            }

        if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            CrLog.log(CrLog.DEBUG, "createAudioRecord(): Audio source chosen: " + source);
            recorder.setSource(audioRecord.getAudioSource());
        }

        if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
            throw new RecordingException("Unable to initialize AudioRecord");

        return audioRecord;
    }

    void disposeAudioRecord() {
        audioRecord.stop();
        audioRecord.release();
    }

    //e statică ca să poată fi apelată din CopyPcmToWav
    static void notifyOnError(Context context) {
        RecorderService service = RecorderService.getService();
        if (service != null) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID,
                        service.buildNotification(RecorderService.RECORD_ERROR,
                                R.string.error_recorder_failed));
        }
    }
}
