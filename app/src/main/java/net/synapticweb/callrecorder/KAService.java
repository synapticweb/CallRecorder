package net.synapticweb.callrecorder;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.TimeUnit;


public class KAService extends IntentService {
    private static final String TAG = "CallRecorder";
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);

    public static void setServiceAlarm(Context context) {
        PendingIntent pi = PendingIntent.getService(
                context, 0, new Intent(context, KAService.class), 0);

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
    }

    public KAService()
    {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        Log.wtf(TAG, "KAService");
    }
}
