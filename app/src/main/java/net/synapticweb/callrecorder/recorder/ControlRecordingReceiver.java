/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.recorder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.settings.SettingsFragment;



public class ControlRecordingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String callIdentifier =  intent.getExtras().getString(RecorderService.CALL_IDENTIFIER);
        RecorderService service = RecorderService.getService();

        if(intent.getAction().equals(RecorderService.ACTION_START_RECORDING)) {
            String phoneNumber = intent.getExtras().getString(RecorderService.PHONE_NUMBER);
            Recorder recorder = service.getRecorder();
            if(recorder != null) {
                try {
                    recorder.startRecording(phoneNumber);
                    if(settings.getBoolean(SettingsFragment.SPEAKER_USE, false))
                        service.putSpeakerOn();
                }
                catch (RecordingException exc) {
                    CrLog.log(CrLog.ERROR, "ControlRecordingReceiver: unable to start recorder: "  + exc.getMessage());
                    service.stopSelf();
                }
            }
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, callIdentifier));
        }
        else if(intent.getAction().equals(RecorderService.ACTION_STOP_SPEAKER)) {
            service.putSpeakerOff();
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, callIdentifier));
        }

        else if(intent.getAction().equals(RecorderService.ACTION_START_SPEAKER)) {
            service.putSpeakerOn();
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, callIdentifier));
        }
    }
}
