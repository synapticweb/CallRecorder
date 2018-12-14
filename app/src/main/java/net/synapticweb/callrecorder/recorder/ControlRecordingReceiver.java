package net.synapticweb.callrecorder.recorder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import net.synapticweb.callrecorder.CallRecorderApplication;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;


public class ControlRecordingReceiver extends BroadcastReceiver {
    private static final String TAG = "CallRecorder";
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(intent.getAction().equals(RecorderBox.ACTION_START_RECORDING)) {
            String callIdentifier =  intent.getExtras().getString(RecorderService.CALL_IDENTIFIER);
            String phoneNumber = intent.getExtras().getString(RecorderService.PHONE_NUMBER);
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, RecorderService.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, callIdentifier));
            RecorderBox.doRecording(CallRecorderApplication.getInstance(), phoneNumber, callIdentifier);
        }
        else if(intent.getAction().equals(RecorderBox.ACTION_STOP_SPEAKER)) {
            String callIdentifier =  intent.getExtras().getString(RecorderService.CALL_IDENTIFIER);
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if(audioManager != null && audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_NORMAL);
                Log.wtf(TAG, "Speaker is on: " + audioManager.isSpeakerphoneOn());
            }
            if(nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID, RecorderService.buildNotification(RecorderService.RECORD_AUTOMMATICALLY, callIdentifier));
        }
    }
}
