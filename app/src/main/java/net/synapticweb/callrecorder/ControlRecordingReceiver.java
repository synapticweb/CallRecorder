package net.synapticweb.callrecorder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;


public class ControlRecordingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, CallRecorderMainActivity.class);
        PendingIntent tapNotificationPi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.record);

        notificationIntent = new Intent(context, ControlRecordingReceiver.class);
        notificationIntent.setAction(RecorderBox.ACTION_STOP_RECORDING);
        PendingIntent stopRecordingPi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);

        if(intent.getAction().equals(RecorderBox.ACTION_STOP_RECORDING))
            context.stopService(new Intent(context, RecorderService.class)); //de văzut: oare oprește corect serviciul?

        else if(intent.getAction().equals(RecorderBox.ACTION_START_RECORDING))
        {
            RecorderBox.doRecording();

            notificationIntent = new Intent(context, ControlRecordingReceiver.class);
            notificationIntent.setAction(RecorderBox.ACTION_PAUSE_RECORDING);
            PendingIntent pauseRecordingPi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);
            String channelId = intent.getExtras().getString("channel_id");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_album_white_24dp)
                    .setContentTitle("CallRecorder")
                    .setContentIntent(tapNotificationPi)
                    .setLargeIcon(bitmap);

            if(Build.VERSION.SDK_INT >= 24)
                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_pause_grey600_24dp, "Pause recording", pauseRecordingPi).build() );

                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_stop_grey600_24dp, "Stop recording", stopRecordingPi).build() )
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Recording..."));

            nm.notify(RecorderService.NOTIFICATION_ID, builder.build());
        }
        else if(intent.getAction().equals(RecorderBox.ACTION_PAUSE_RECORDING))
        {
            RecorderBox.pauseRecorder();

            notificationIntent = new Intent(context, ControlRecordingReceiver.class);
           notificationIntent.setAction(RecorderBox.ACTION_RESUME_RECORDING);
            PendingIntent resumeRecordingPi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);


            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_album_white_24dp)
                    .setContentTitle("CallRecorder")
//                    .setContentText("Recording...")
                    .setContentIntent(tapNotificationPi)
                    .setLargeIcon(bitmap)
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_play_grey600_24dp, "Resume recording", resumeRecordingPi).build() )
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_stop_grey600_24dp, "Stop recording", stopRecordingPi).build() )
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Paused recording."));

            nm.notify(RecorderService.NOTIFICATION_ID, builder.build());
        }
        else if(intent.getAction().equals(RecorderBox.ACTION_RESUME_RECORDING))
        {
            RecorderBox.resumeRecorder();

            notificationIntent = new Intent(context, ControlRecordingReceiver.class);
           notificationIntent.setAction(RecorderBox.ACTION_PAUSE_RECORDING);
            PendingIntent pauseRecordingPi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_album_white_24dp)
                    .setContentTitle("CallRecorder")
//                    .setContentText("Recording...")
                    .setContentIntent(tapNotificationPi)
                    .setLargeIcon(bitmap)
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_pause_grey600_24dp, "Pause recording", pauseRecordingPi).build() )
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_stop_grey600_24dp, "Stop recording", stopRecordingPi).build() )
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Resumed recording."));

            nm.notify(RecorderService.NOTIFICATION_ID, builder.build());
        }
    }
}
