package net.synapticweb.callrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
//import android.support.v4.media.app.NotificationCompat.MediaStyle;device

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.HashMap;
import java.util.Map;

import net.synapticweb.callrecorder.databases.ListenedContract.*;
import net.synapticweb.callrecorder.databases.RecordingsContract.*;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;
import static net.synapticweb.callrecorder.GlobalConstants.*;


public class RecorderService extends Service {
    private static final String TAG = "CallRecorder";
    private String receivedNumPhone;
    private String dbNumPhone = null;
    private Boolean incoming;

    public static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "call_recorder_channel";
    private boolean unknownPhone = false;
    private boolean privateCall = false;
    private PhoneNumber phoneNumber = null;


    @Override
    public IBinder onBind(Intent i)
    {
        return null;
    }

    public void onCreate()
    {
        super.onCreate();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // The user-visible name of the channel.
        CharSequence name = "Call recorder";
        // The user-visible description of the channel.
        String description = "Call recorder controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    private Notification buildNotification(boolean startRecording) {
        Intent notificationIntent = new Intent(this, CallRecorderMainActivity.class);
        PendingIntent tapNotificationPi = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.record);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel();
        NotificationCompat.Builder builder;

        if(startRecording)
        {
            notificationIntent = new Intent(this, ControlRecordingReceiver.class);
            notificationIntent.setAction(RecorderBox.ACTION_PAUSE_RECORDING);
            PendingIntent pauseRecordingPi = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

            notificationIntent = new Intent(this, ControlRecordingReceiver.class);
            notificationIntent.setAction(RecorderBox.ACTION_STOP_RECORDING);
            PendingIntent stopRecordingPi = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_album_white_24dp)
                    .setContentTitle("CallRecorder")
                    .setContentIntent(tapNotificationPi)
                    .setLargeIcon(bitmap);

            if (Build.VERSION.SDK_INT >= 24)
                builder.addAction(new NotificationCompat.Action.Builder(
                        R.drawable.ic_pause_grey600_24dp, "Pause recording", pauseRecordingPi).build());

            builder.addAction(new NotificationCompat.Action.Builder(
                    R.drawable.ic_stop_grey600_24dp, "Stop recording", stopRecordingPi).build())
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("Recording..."));

        }
        else
        {
            notificationIntent = new Intent(this, ControlRecordingReceiver.class);
            notificationIntent.setAction(RecorderBox.ACTION_START_RECORDING);
            notificationIntent.putExtra("channel_id", CHANNEL_ID);
            PendingIntent startRecordingPi = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_album_white_24dp)
                    .setContentTitle("CallRecorder")
                    .setContentIntent(tapNotificationPi)
                    .setLargeIcon(bitmap)
                    .addAction(new NotificationCompat.Action.Builder(R.drawable.ic_play_grey600_24dp,
                            "Start recording", startRecordingPi).build() )
                    .setStyle(new NotificationCompat.BigTextStyle()); //Fiindcă încă nu înregistrăm, nu scriem nimic deasupra
        }
        return builder.build();
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Map<String, Boolean> numbers = new HashMap<>();

        super.onStartCommand(intent, flags, startId);

        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {Listened.COLUMN_NAME_NUMBER, Listened.COLUMN_NAME_SHOULD_RECORD};
        Cursor cursor = db.query(
                Listened.TABLE_NAME, projection, null, null, null, null, null);

        while(cursor.moveToNext())
        {
            String number = cursor.getString(cursor.getColumnIndex(Listened.COLUMN_NAME_NUMBER));
            Boolean shouldRecord = cursor.getInt(cursor.getColumnIndex(Listened.COLUMN_NAME_SHOULD_RECORD)) == 1;
            numbers.put(number, shouldRecord);
        }
        cursor.close();

        receivedNumPhone = intent.getStringExtra("phoneNumber");
        incoming = intent.getBooleanExtra("incoming", false);
        RecorderBox.setAudioFile(this, receivedNumPhone);
        boolean match = false;

        if(receivedNumPhone == null) //în cazul în care nr primit e null înseamnă că se sună de pe nr privat
            privateCall = true;

        if(!privateCall) { //și nu trebuie să mai verificăm dacă nr este în baza de date sau dacă nu
            // este în baza de date dacă este în contacte.
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            for (Map.Entry<String, Boolean> entry : numbers.entrySet()) {
                dbNumPhone = entry.getKey();
                PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(receivedNumPhone, dbNumPhone);
                if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                    match = true;
                    break;
                }
            }
            if(!match) { //chiar dacă nu se găsește în db,  s-ar putea să fie contacte. Verificăm chestia asta
                // și dacă îl găsim în contacte populăm RecorderService.phoneNumber.
                if ((phoneNumber = PhoneNumber.searchNumberInContacts(receivedNumPhone, getApplicationContext())) == null)
                    unknownPhone = true; //dacă nu e nici în contacte îl declarăm nr. necunoscut.
            }
        }

        //dacă nr nu există în db sau dacă setările interzic înregistrarea convorbirilor nr punem doar o notificare
        //cu posibilitatea de a porni înregistrarea. Dacă s-a găsit un match, atunci dbNumPhone este setat la nr din baza de date care
        //se potrivește
        if(!match || !numbers.get(dbNumPhone))
            startForeground(NOTIFICATION_ID, buildNotification(false));
        else {
            startForeground(NOTIFICATION_ID, buildNotification(true)); //altfel, pornim înregistrarea și notificarea conține
            //buton pentru oprire.
            RecorderBox.doRecording();
        }
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        RecorderBox.disposeRecorder();

        if(!RecorderBox.getRecordingDone()) //dacă nu s-a pornit înregistrarea nu avem nimic de făcut
            return ;

        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        long idToInsert;

        if(unknownPhone)
        {
           PhoneNumber phoneNumber =  new PhoneNumber(null, receivedNumPhone, null, null, -1);
           phoneNumber.setUnkownNumber(true);
           try {
               phoneNumber.insertInDatabase(this); //introducerea în db setează id-ul în obiect
           }
           catch (SQLException exc) {
               Log.wtf(TAG, exc.getMessage());
           }
           idToInsert = phoneNumber.getId();
        }
        else if(privateCall)
        {
            Cursor cursor = db.query(Listened.TABLE_NAME, new String[]{Listened._ID},
                    Listened.COLUMN_NAME_PRIVATE_NUMBER + "=" + SQLITE_TRUE, null, null, null, null);

            if(cursor.getCount() == 0) {
                PhoneNumber phoneNumber =  new PhoneNumber();
                phoneNumber.setPrivateNumber(true);
                phoneNumber.setContactName(null);
                try {
                    phoneNumber.insertInDatabase(this);
                }
                catch (SQLException exc) {
                    Log.wtf(TAG, exc.getMessage());
                }
                idToInsert = phoneNumber.getId();
            }
            else {
                cursor.moveToFirst();
                idToInsert = cursor.getInt(cursor.getColumnIndex(Listened._ID));
            }

            cursor.close();
        }
        else //dacă nu e nici unknown nici privat: se poate ca nr să existe în db, sau să nu existe dar să fi fost
        // găsit în contacte.
        {
            if(phoneNumber != null) //în cazul în care nr nu exista în db dar a fost găsit în contacte și deci
                //RecorderService::phoneNumber e nonnul, inserăm în db numărul găsit
            {
                try {
                    phoneNumber.insertInDatabase(this);
                }
                catch (SQLException exception) {
                    Log.wtf(TAG, exception.getMessage());
                }
                idToInsert = phoneNumber.getId();
            }
            else { //dacă nr există în baza de date

                Cursor cursor = db.query(Listened.TABLE_NAME, new String[]{Listened._ID},
                        Listened.COLUMN_NAME_NUMBER + "='" + dbNumPhone + "'", null, null, null, null);
                cursor.moveToFirst();
                idToInsert = cursor.getLong(cursor.getColumnIndex(Listened._ID));
                cursor.close();
            }
        }

        values.clear();
        values.put(Recordings.COLUMN_NAME_PHONE_NUM_ID, idToInsert);
        values.put(Recordings.COLUMN_NAME_INCOMING, incoming ? SQLITE_TRUE : SQLITE_FALSE);
        values.put(Recordings.COLUMN_NAME_PATH, RecorderBox.getAudioFilePath());
        values.put(Recordings.COLUMN_NAME_START_TIMESTAMP, RecorderBox.getStartTimestamp());
        values.put(Recordings.COLUMN_NAME_END_TIMESTAMP, System.currentTimeMillis());

        db.insert(Recordings.TABLE_NAME, null, values);

    }
}
