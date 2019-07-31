package net.synapticweb.callrecorder.recorder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.preference.PreferenceManager;
import android.util.Log;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.settings.SettingsFragment;


public class ControlRecordingReceiver extends BroadcastReceiver {
    private static final String TAG = "CallRecorder";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String callIdentifier =  intent.getExtras().getString(RecorderService.CALL_IDENTIFIER);
        RecorderService service = RecorderService.getService();

        if(intent.getAction().equals(RecorderService.ACTION_START_RECORDING)) {
            String phoneNumber = intent.getExtras().getString(RecorderService.PHONE_NUMBER);
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, callIdentifier));

            Recorder recorder = service.getRecorder();
            if(recorder != null) {
                try {
                    recorder.startRecording(phoneNumber);
                    if(settings.getBoolean(SettingsFragment.SPEAKER_USE, false))
                        service.putSpeakerOn();
                }
                catch (RecordingException exc) {
                    Log.wtf(TAG, "Unable to start recorder: "  + exc.getMessage());
                    service.stopSelf();
                }
            }
        }
        else if(intent.getAction().equals(RecorderService.ACTION_STOP_SPEAKER)) {
            service.putSpeakerOff();
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, service.buildNotification(RecorderService.RECORD_AUTOMMATICALLY_SPEAKER_OFF, callIdentifier));
        }
    }
}
