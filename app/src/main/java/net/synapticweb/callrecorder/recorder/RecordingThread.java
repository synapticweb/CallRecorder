/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.recorder;


import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class RecordingThread {
    protected static final String TAG = "CallRecorder";
    static final int SAMPLE_RATE = 44100;
    final int channels;
    final int bufferSize;
    final AudioRecord audioRecord;

    RecordingThread(String mode) throws RecordingException {
        channels = (mode.equals(Recorder.MONO) ? 1 : 2);
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = createAudioRecord();
        audioRecord.startRecording();
    }

    @SuppressLint("newApi")
    private AudioRecord createAudioRecord() throws RecordingException {
        AudioRecord audioRecord = null;
        List<Integer> audioSources = new ArrayList<>(Arrays.asList(
                MediaRecorder.AudioSource.VOICE_CALL,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.DEFAULT
                ));

        AudioManager am = (AudioManager) CrApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
        if(am != null && am.getProperty("PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED") != null)
            audioSources.add(1, MediaRecorder.AudioSource.UNPROCESSED);

        for(int source : audioSources) {
            try {
                audioRecord = new AudioRecord(source, SAMPLE_RATE,
                        channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
            } catch (Exception e) { //La VOICE_CALL dă IllegalArgumentException. Aplicația nu se oprește, rămîne
                //hanging, nu înregistrează nimic.
                continue;
            }

            if(audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                CrLog.log(CrLog.DEBUG, "createAudioRecord(): Audio source chosen: " + source);
                break;
            }
        }

        if(audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
            throw new RecordingException("Unable to initialize AudioRecord");

        return audioRecord;
    }

    void disposeAudioRecord() {
        audioRecord.stop();
        audioRecord.release();
    }
}
