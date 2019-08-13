package net.synapticweb.callrecorder.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
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

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsContract.*;
import net.synapticweb.callrecorder.data.RecordingsContract.*;
import net.synapticweb.callrecorder.data.CallRecorderDbHelper;
import net.synapticweb.callrecorder.settings.SettingsFragment;


public class RecorderService extends Service {
    private static final String TAG = "CallRecorder";
    private  String receivedNumPhone = null;
    private  Boolean privateCall = null;
    private  Boolean match = null;
    private  Boolean incoming = null;
    public  boolean shouldStartAtHookup = false;
    private Long idIfMatch = null;
    private  String contactNameIfMatch = null;
    private  Recorder recorder;
    private  SharedPreferences settings;
    private  Thread speakerOnThread;
    private  AudioManager audioManager;
    private static RecorderService self;

    public static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "call_recorder_channel";
    public static final String CALL_IDENTIFIER = "call_identifier";
    public static final String PHONE_NUMBER = "phone_number";

    public static final int RECORD_AUTOMMATICALLY = 1;
    public static final int RECORD_ON_HOOKUP = 2;
    public static final int RECORD_ON_REQUEST = 3;
    public static final int RECORD_AUTOMMATICALLY_SPEAKER_OFF = 4;
    static final String ACTION_START_RECORDING = "net.synapticweb.callrecorder.START_RECORDING";
    static final String ACTION_STOP_SPEAKER = "net.synapticweb.callrecorder.STOP_SPEAKER";

    @Override
    public IBinder onBind(Intent i){
        return null;
    }

    public void onCreate(){
        super.onCreate();
        recorder = new Recorder();
        settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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

    public Notification buildNotification(int typeOfNotification, String callNameOrNumber) {
        Intent notificationIntent = new Intent(CrApp.getInstance(), ContactsListActivityMain.class);
        PendingIntent tapNotificationPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, notificationIntent, 0);
        Resources res = CrApp.getInstance().getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(CrApp.getInstance(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(callNameOrNumber + (incoming ? " (incoming)" : " (outgoing)"))
                .setContentIntent(tapNotificationPi);

        String callIdentifier;
        if(privateCall)
            callIdentifier = CrApp.getInstance().getResources().getString(R.string.private_number_name);
        else
            callIdentifier = match ? contactNameIfMatch : receivedNumPhone;

        switch(typeOfNotification) {
            case RECORD_AUTOMMATICALLY_SPEAKER_OFF:
                builder.setContentText(res.getString(R.string.recording));
                break;
            case RECORD_AUTOMMATICALLY:
                if(settings.getBoolean(SettingsFragment.SPEAKER_USE, false)) {
                    notificationIntent = new Intent(CrApp.getInstance(), ControlRecordingReceiver.class);
                    notificationIntent.setAction(ACTION_STOP_SPEAKER);
                    notificationIntent.putExtra(CALL_IDENTIFIER, callIdentifier);
                    PendingIntent stopSpeakerPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, notificationIntent, 0);
                    //de schimbat icoana!!!
                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_play_grey600_24dp,
                            res.getString(R.string.stop_speaker), stopSpeakerPi).build() )
                    .setContentText(res.getString(R.string.recording_speaker_on));
                }
                else
                    builder.setContentText(res.getString(R.string.recording));
                break;
            case RECORD_ON_HOOKUP:
                builder.setContentText(res.getString(R.string.recording_answer_call));
                break;
            case RECORD_ON_REQUEST:
                notificationIntent = new Intent(CrApp.getInstance(), ControlRecordingReceiver.class);
                notificationIntent.setAction(ACTION_START_RECORDING);
                notificationIntent.putExtra(CALL_IDENTIFIER, callIdentifier);
                notificationIntent.putExtra(PHONE_NUMBER, receivedNumPhone != null ? receivedNumPhone : "private_phone");
                PendingIntent startRecordingPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, notificationIntent, 0);
                builder.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_play_grey600_24dp,
                                res.getString(R.string.start_recording_notification), startRecordingPi).build() )
                        .setContentText(res.getString(R.string.start_recording_notification_text));
        }

        return builder.build();
    }

    public void onIncomingOfhook() {
        CrLog.log(CrLog.DEBUG, "onIncomingOfhook() called");
        if(shouldStartAtHookup) {
            NotificationManager nm = (NotificationManager) CrApp.getInstance().
                    getSystemService(Context.NOTIFICATION_SERVICE);
            String callIdentifier;
            if(privateCall)
                callIdentifier = CrApp.getInstance().getResources().getString(R.string.private_number_name);
            else
                callIdentifier = match ? contactNameIfMatch : receivedNumPhone;
            if(nm != null)
                nm.notify(NOTIFICATION_ID, buildNotification(RECORD_AUTOMMATICALLY, callIdentifier));

            try {
                recorder.startRecording(receivedNumPhone);
                if(settings.getBoolean(SettingsFragment.SPEAKER_USE, false))
                    putSpeakerOn();
            }
            catch (RecordingException e) {
                CrLog.log(CrLog.ERROR, "onIncomingOfhook: unable to start recording: " + e.getMessage() + " Stoping the service...");
                stopSelf();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        boolean shouldRecord = true;

        receivedNumPhone = intent.getStringExtra(CallReceiver.ARG_NUM_PHONE);
        incoming = intent.getBooleanExtra(CallReceiver.ARG_INCOMING, false);
        CrLog.log(CrLog.DEBUG, String.format("Recorder service started. Phone number: %s. Incoming: %s", receivedNumPhone, incoming));

        //în cazul în care nr primit e null înseamnă că se sună de pe nr privat
        privateCall = (receivedNumPhone == null);

        if(!privateCall) {//și nu trebuie să mai verificăm dacă nr este în baza de date sau, dacă nu
            // este în baza de date, dacă este în contacte.
            Contact contact;
            match = ((contact = Contact.getContactIfNumberInDb(receivedNumPhone, CrApp.getInstance())) != null);
            if(match) {
                idIfMatch = contact.getId(); //pentru teste: idIfMatch nu trebuie să fie niciodată null dacă match == true
                contactNameIfMatch = contact.getContactName(); //posibil subiect pentru un test.
                shouldRecord = contact.shouldRecord();
            }
            else { //în caz de ussd serviciul se oprește
                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                String countryCode = CrApp.getUserCountry(CrApp.getInstance());
                if(countryCode == null)
                    countryCode = "US";
                try {
                    phoneUtil.parse(receivedNumPhone, countryCode);
                }
                catch (NumberParseException exc) {
                    stopSelf();
                }
            }
        }
        boolean recordAutoPrivCalls = settings.getBoolean(SettingsFragment.AUTOMMATICALLY_RECORD_PRIVATE_CALLS, false);
        boolean paranoidMode = settings.getBoolean(SettingsFragment.PARANOID_MODE, false);

        if(incoming) {
            if(privateCall) {
                if(recordAutoPrivCalls || paranoidMode){
                    startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_HOOKUP,
                            getResources().getString(R.string.private_number_name)));
                    shouldStartAtHookup = true;
                }
                else
                    startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_REQUEST,
                            getResources().getString(R.string.private_number_name)));
            }
            else { //normal call, number present.
                if(match || paranoidMode) {
                    if(paranoidMode)
                        shouldRecord = true;
                    if(shouldRecord) {
                        startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_HOOKUP, contactNameIfMatch
                                != null ? contactNameIfMatch : receivedNumPhone));
                        shouldStartAtHookup = true;
                    }
                    else // shouldRecord este false. Deci nu este paranoid mode, deci este match. Tertium non datur.
                    //Dacă este match, contactNameIfMatch != null:
                        startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_REQUEST, contactNameIfMatch));
                }
                else //nu este nici match nici paranoid mode.
                    startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_REQUEST, receivedNumPhone));
            }
        }
        else { //outgoing call
            if(match || paranoidMode) {
                if(paranoidMode)
                    shouldRecord = true;
                if(shouldRecord) {
                    String callIdentifier = contactNameIfMatch != null ? contactNameIfMatch : receivedNumPhone;
                    startForeground(NOTIFICATION_ID, buildNotification(RECORD_AUTOMMATICALLY, callIdentifier));

                    try {
                        recorder.startRecording(receivedNumPhone);
                        if(settings.getBoolean(SettingsFragment.SPEAKER_USE, false))
                            putSpeakerOn();
                    }
                    catch (RecordingException e) {
                        CrLog.log(CrLog.ERROR, "onStartCommand: unable to start recorder: " + e.getMessage() + " Stoping the service...");
                        stopSelf();
                    }
                }
                else //ca mai sus
                    startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_REQUEST, contactNameIfMatch));
            }
            else //nici match nici paranoid mode
                startForeground(NOTIFICATION_ID, buildNotification(RECORD_ON_REQUEST, receivedNumPhone));
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
                    while(true) {
                        sleep(1000);
                        audioManager.setMode(AudioManager.MODE_IN_CALL);
                        if (!audioManager.isSpeakerphoneOn())
                            audioManager.setSpeakerphoneOn(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        speakerOnThread.start();
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
        Long idToInsert;

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
                idToInsert = contact.getId();
            }
            else { //Avem cel puțin un apel de pe nr ascuns. Pentru teste: aici e de așteptat ca întotdeauna cursorul să conțină numai 1 element
                cursor.moveToFirst();
                idToInsert = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
            }
            cursor.close();
        }

        else if(match)
            idToInsert = idIfMatch;

        else { //dacă nu e nici match nici private atunci trebuie mai întîi verificat dacă nu cumva nr există totuși în contactele telefonului.
            Contact contact;
            if((contact = Contact.searchNumberInPhoneContacts(receivedNumPhone, getApplicationContext())) != null) {
                try {
                    contact.insertInDatabase(this);
                }
                catch (SQLException exception) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exception.getMessage());
                }
                idToInsert = contact.getId();
            }
            else { //numărul nu există nici contactele telefonului. Deci este unknown.
                contact =  new Contact(null, receivedNumPhone, getResources().getString(R.string.unkown_contact), null, CrApp.UNKNOWN_TYPE_PHONE_CODE);
                try {
                    contact.insertInDatabase(this); //introducerea în db setează id-ul în obiect
                }
                catch (SQLException exc) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
                }
                idToInsert = contact.getId();
            }
        }

        if(idToInsert == null) {
            CrLog.log(CrLog.ERROR, "Error at obtaining contact id. No contact inserted. Aborted.");
            resetState();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(Recordings.COLUMN_NAME_PHONE_NUM_ID, idToInsert);
        values.put(Recordings.COLUMN_NAME_INCOMING, incoming ? CrApp.SQLITE_TRUE : CrApp.SQLITE_FALSE);
        values.put(Recordings.COLUMN_NAME_PATH, recorder.getAudioFilePath());
        values.put(Recordings.COLUMN_NAME_START_TIMESTAMP, recorder.getStartingTime());
        values.put(Recordings.COLUMN_NAME_END_TIMESTAMP, System.currentTimeMillis());
        values.put(Recordings.COLUMN_NAME_FORMAT, recorder.getFormat());
        values.put(Recordings.COLUMN_NAME_MODE, recorder.getMode());

        try {
            db.insert(Recordings.TABLE_NAME, null, values);
        }
        catch(SQLException exc) {
            CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
        }

        resetState();
    }
}
