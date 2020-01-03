/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsContract.*;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.data.CallRecorderDbHelper;
import net.synapticweb.callrecorder.settings.SettingsFragment;

import org.acra.ACRA;


public class RecorderService extends Service {
    private  String receivedNumPhone = null;
    private  boolean privateCall = false;
    private  Boolean incoming = null;
    private  Recorder recorder;
    private  Thread speakerOnThread;
    private  AudioManager audioManager;
    private static RecorderService self;
    private boolean speakerOn = false;
    private Contact contact = null;
    private String callIdentifier;
    private SharedPreferences settings;

    public static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "call_recorder_channel";
    public static final String PHONE_NUMBER = "phone_number";
    public static final int RECORD_AUTOMMATICALLY = 1;
    public static final int RECORD_ERROR = 4;

    static final String ACTION_START_RECORDING = "net.synapticweb.callrecorder.START_RECORDING";
    static final String ACTION_STOP_SPEAKER = "net.synapticweb.callrecorder.STOP_SPEAKER";
    static final String ACTION_START_SPEAKER = "net.synapticweb.callrecorder.START_SPEAKER";

    static final String ACRA_PHONE_NUMBER = "phone_number";
    static final String ACRA_INCOMING = "incoming";

    @Override
    public IBinder onBind(Intent i){
        return null;
    }

    public void onCreate(){
        super.onCreate();
        recorder = new Recorder();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
        self = this;
    }

    public static RecorderService getService() {
        return self;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void createChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) CrApp.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

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

    public Notification buildNotification(int typeOfNotification) {
        Intent notificationIntent = new Intent(CrApp.getInstance(), ContactsListActivityMain.class);
        PendingIntent tapNotificationPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, notificationIntent, 0);
        Resources res = CrApp.getInstance().getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(CrApp.getInstance(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(callIdentifier + (incoming ? " (incoming)" : " (outgoing)"))
                .setContentIntent(tapNotificationPi);

        switch (typeOfNotification) {
                //Acum nu se mai bazează pe speakerOn, recunoaște dacă difuzorul era deja pornit. speakerOn
                //a fost menținut deoarece în unele situații notificarea porneste prea devreme și isSpeakerphoneOn()
                //returnează false.
            case RECORD_AUTOMMATICALLY:
                if (audioManager.isSpeakerphoneOn() || speakerOn) {
                    notificationIntent = new Intent(CrApp.getInstance(), ControlRecordingReceiver.class);
                    notificationIntent.setAction(ACTION_STOP_SPEAKER);
                    PendingIntent stopSpeakerPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, notificationIntent, 0);
                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.speaker_phone_off,
                            res.getString(R.string.stop_speaker), stopSpeakerPi).build())
                            .setContentText(res.getString(R.string.recording_speaker_on));
                } else {
                    notificationIntent = new Intent(CrApp.getInstance(), ControlRecordingReceiver.class);
                    notificationIntent.setAction(ACTION_START_SPEAKER);
                    PendingIntent startSpeakerPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, notificationIntent, 0);
                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.speaker_phone_on,
                            res.getString(R.string.start_speaker), startSpeakerPi).build())
                            .setContentText(res.getString(R.string.recording_speaker_off));
                }
                break;

            case RECORD_ERROR: builder.setContentText(res.getString(R.string.error_recorder_notif));
        }

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(intent.hasExtra(CallReceiver.ARG_NUM_PHONE))
            receivedNumPhone = intent.getStringExtra(CallReceiver.ARG_NUM_PHONE);
        incoming = intent.getBooleanExtra(CallReceiver.ARG_INCOMING, false);
        CrLog.log(CrLog.DEBUG, String.format("Recorder service started. Phone number: %s. Incoming: %s", receivedNumPhone, incoming));
        try {
            ACRA.getErrorReporter().putCustomData(ACRA_PHONE_NUMBER, receivedNumPhone);
            ACRA.getErrorReporter().putCustomData(ACRA_INCOMING, incoming.toString());
        }
        catch (IllegalStateException ignored) {
        }
        //de văzut dacă formarea ussd-urilor trimite ofhook dacă nu mai primim new_outgoing_call

        if(receivedNumPhone == null && incoming && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            privateCall = true;

        //se întîmplă numai la incoming, la outgoing totdeauna nr e null.
        if(receivedNumPhone != null) {
            contact = Contact.queryNumberInAppContacts(receivedNumPhone, CrApp.getInstance());
            if(contact == null) {
                contact = Contact.queryNumberInPhoneContacts(receivedNumPhone, CrApp.getInstance());
                if(contact == null) {
                    contact = new Contact(null, receivedNumPhone, getResources().getString(R.string.unkown_contact), null, CrApp.UNKNOWN_TYPE_PHONE_CODE);
                }

                try {
                    contact.insertInDatabase(this);
                }
                catch (SQLException exception) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exception.getMessage());
                }
            }
        }

        if(contact != null) {
            String name = contact.getContactName();
            callIdentifier = name.equals(getResources().getString(R.string.unkown_contact)) ?
                   receivedNumPhone : name;
        }
        else if(privateCall)
            callIdentifier = getResources().getString(R.string.private_number_name);
        else
            callIdentifier = getResources().getString(R.string.unknown_number);

        try {
            CrLog.log(CrLog.DEBUG, "Recorder started in onStartCommand()");
            recorder.startRecording(receivedNumPhone);
            if (settings.getBoolean(SettingsFragment.SPEAKER_USE, false))
                putSpeakerOn();
            startForeground(NOTIFICATION_ID, buildNotification(RECORD_AUTOMMATICALLY));
        }
         catch (RecordingException e) {
             CrLog.log(CrLog.ERROR, "onStartCommand: unable to start recorder: " + e.getMessage() + " Stoping the service...");
             Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_recorder_fail), Toast.LENGTH_LONG).show();
             startForeground(NOTIFICATION_ID, buildNotification(RECORD_ERROR));
         }

        return START_NOT_STICKY;
    }

    private void resetState() {
        self = null;
    }

    //de aici: https://stackoverflow.com/questions/39725367/how-to-turn-on-speaker-for-incoming-call-programmatically-in-android-l
    void putSpeakerOn() {
        speakerOnThread =  new Thread() {
            @Override
            public void run() {
                CrLog.log(CrLog.DEBUG, "Speaker has been turned on");
                try {
                    while(!Thread.interrupted()) {
                        audioManager.setMode(AudioManager.MODE_IN_CALL);
                        if (!audioManager.isSpeakerphoneOn())
                            audioManager.setSpeakerphoneOn(true);
                        sleep(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        speakerOnThread.start();
        speakerOn = true;
    }

    void putSpeakerOff() {
        if(speakerOnThread != null) {
            speakerOnThread.interrupt();
            CrLog.log(CrLog.DEBUG, "Speaker has been turned off");
        }
        speakerOnThread = null;
        if (audioManager != null && audioManager.isSpeakerphoneOn()) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
        speakerOn = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CrLog.log(CrLog.DEBUG, "RecorderService is stoping now...");

        putSpeakerOff();
        if(!recorder.isRunning()) {
            resetState();
            return;
        }

        recorder.stopRecording();

        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Long contactId;

        if(privateCall) {
            Cursor cursor = db.query(Contacts.TABLE_NAME, new String[]{Contacts._ID},
                    Contacts.COLUMN_NAME_PRIVATE_NUMBER + "=" + CrApp.SQLITE_TRUE, null, null, null, null);

            if(cursor.getCount() == 0) { //încă nu a fost înregistrat un apel de pe număr ascuns
                Contact contact =  new Contact();
                contact.setPrivateNumber(true);
                try {
                    contact.insertInDatabase(this);
                }
                catch (SQLException  exc) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
                }
                contactId = contact.getId();
            }
            else { //Avem cel puțin un apel de pe nr ascuns. Pentru teste: aici e de așteptat ca întotdeauna cursorul să conțină numai 1 element
                cursor.moveToFirst();
                contactId = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
            }
            cursor.close();
        }

        else if(contact != null)
            contactId = contact.getId();

        else  //dacă nu e privat și contactul este null atunci nr e indisponibil.
            contactId = null;

        Recording recording = new Recording(null, contactId, recorder.getAudioFilePath(), incoming,
                recorder.getStartingTime(), System.currentTimeMillis(), recorder.getFormat(), false, recorder.getMode(),
                recorder.getSource());

        try {
            recording.insertInDatabase(CrApp.getInstance());
        }
        catch(SQLException exc) {
            CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
        }

        resetState();
        try {
            ACRA.getErrorReporter().clearCustomData();
        }
        catch (IllegalStateException ignored) {
        }
    }
}
