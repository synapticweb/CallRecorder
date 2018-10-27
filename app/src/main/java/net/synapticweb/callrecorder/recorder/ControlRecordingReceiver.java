package net.synapticweb.callrecorder.recorder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import net.synapticweb.callrecorder.CallRecorderApplication;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;


public class ControlRecordingReceiver extends BroadcastReceiver {
    private static final String TAG = "CallRecorder";
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, ContactsListActivityMain.class);
        PendingIntent tapNotificationPi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.record);

        if(intent.getAction().equals(RecorderBox.ACTION_START_RECORDING)) {
            String channelId = intent.getExtras().getString(RecorderService.CHANNEL_ID_KEY);
            String phoneNumber =  intent.getExtras().getString(RecorderService.PHONE_NUM_KEY);
            RecorderBox.doRecording(CallRecorderApplication.getInstance(), phoneNumber);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_album_white_24dp)
                    .setContentTitle("CallRecorder")
                    .setContentIntent(tapNotificationPi)
                    .setLargeIcon(bitmap)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Recording..."));;

            nm.notify(RecorderService.NOTIFICATION_ID, builder.build());
        }
    }
}
