/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
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
import android.database.SQLException;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.data.Repository;
import net.synapticweb.callrecorder.settings.SettingsFragment;
import org.acra.ACRA;
import javax.inject.Inject;

public class RecorderService extends Service {
    private  String receivedNumPhone = null;
    private  boolean privateCall = false;
    private  Boolean incoming = null;
    private  Recorder recorder;
    private  Thread speakerOnThread;
    private  AudioManager audioManager;
    private NotificationManager nm;
    private static RecorderService self;
    private boolean speakerOn = false;
    private Contact contact = null;
    private String callIdentifier;
    private SharedPreferences settings;
    @Inject
    Repository repository;

    public static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "call_recorder_channel";
    public static final int RECORD_AUTOMMATICALLY = 1;
    public static final int RECORD_ERROR = 4;
    public static final int RECORD_SUCCESS = 5;
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
        recorder = new Recorder(getApplicationContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ((CrApp) getApplication()).appComponent.inject(this);
        self = this;
    }

    public static RecorderService getService() {
        return self;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
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
        nm.createNotificationChannel(mChannel);
    }

    public Notification buildNotification(int typeOfNotification, int message) {
        Intent goToActivity = new Intent(getApplicationContext(), ContactsListActivityMain.class);
        PendingIntent tapNotificationPi = PendingIntent.getActivity(getApplicationContext(), 0, goToActivity, 0);
        Intent sendBroadcast = new Intent(getApplicationContext(), ControlRecordingReceiver.class);
        Resources res = getApplicationContext().getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(callIdentifier + (incoming ? " (incoming)" : " (outgoing)"))
                .setContentIntent(tapNotificationPi);

        switch (typeOfNotification) {
                //Acum nu se mai bazează pe speakerOn, recunoaște dacă difuzorul era deja pornit. speakerOn
                //a fost menținut deoarece în unele situații notificarea porneste prea devreme și isSpeakerphoneOn()
                //returnează false.
            case RECORD_AUTOMMATICALLY:
                if (audioManager.isSpeakerphoneOn() || speakerOn) {
                    sendBroadcast.setAction(ACTION_STOP_SPEAKER);
                    PendingIntent stopSpeakerPi = PendingIntent.getBroadcast(CrApp.getInstance(), 0, sendBroadcast, 0);
                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.speaker_phone_off,
                            res.getString(R.string.stop_speaker), stopSpeakerPi).build())
                            .setContentText(res.getString(R.string.recording_speaker_on));
                } else {
                    sendBroadcast.setAction(ACTION_START_SPEAKER);
                    PendingIntent startSpeakerPi = PendingIntent.getBroadcast(getApplicationContext(), 0, sendBroadcast, 0);
                    builder.addAction(new NotificationCompat.Action.Builder(R.drawable.speaker_phone_on,
                            res.getString(R.string.start_speaker), startSpeakerPi).build())
                            .setContentText(res.getString(R.string.recording_speaker_off));
                }
                break;

            case RECORD_ERROR: builder.setColor(Color.RED)
                    .setColorized(true)
                    .setSmallIcon(R.drawable.notification_icon_error)
                    .setContentTitle(res.getString(R.string.error_notification_title))
                    .setContentText(res.getString(message))
                    .setAutoCancel(true);
                break;

            case RECORD_SUCCESS: builder.setSmallIcon(R.drawable.notification_icon_success)
                   .setContentText(res.getString(R.string.notification_success))
                   .setAutoCancel(true);
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
            contact = Contact.queryNumberInAppContacts(repository, receivedNumPhone);
            if(contact == null) {
                contact = Contact.queryNumberInPhoneContacts(receivedNumPhone, getContentResolver());
                if(contact == null) {
                    contact = new Contact(null, receivedNumPhone, getResources().getString(R.string.unkown_contact), null, Util.UNKNOWN_TYPE_PHONE_CODE);
                }
                else if(contact.getPhotoUri() != null)
                    Util.copyPhotoFromPhoneContacts(getApplicationContext(), contact);

                try {
                    contact.save(repository);
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
            startForeground(NOTIFICATION_ID, buildNotification(RECORD_AUTOMMATICALLY, 0));
        }
         catch (RecordingException e) {
             CrLog.log(CrLog.ERROR, "onStartCommand: unable to start recorder: " + e.getMessage() + " Stoping the service...");
             startForeground(NOTIFICATION_ID, buildNotification(RECORD_ERROR, R.string.error_recorder_cannot_start));
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
        if(!recorder.isRunning() || recorder.hasError()) {
            onDestroyCleanUp();
            return;
        }

        recorder.stopRecording();
        Long contactId;

        if(privateCall) {
            contactId = repository.getHiddenNumberContactId();
            if(contactId == null) { //încă nu a fost înregistrat un apel de pe număr ascuns
                Contact contact =  new Contact();
                contact.setIsPrivateNumber();
                contact.setContactName(getApplicationContext().getString(R.string.private_number_name));
                try {
                    contact.save(repository);
                }
                catch (SQLException  exc) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
                    onDestroyCleanUp();
                    return ;
                }
                contactId = contact.getId();
            }
        }

        else if(contact != null)
            contactId = contact.getId();

        else  //dacă nu e privat și contactul este null atunci nr e indisponibil.
            contactId = null;

        Recording recording = new Recording(null, contactId, recorder.getAudioFilePath(), incoming,
                recorder.getStartingTime(), System.currentTimeMillis(), recorder.getFormat(), false, recorder.getMode(),
                recorder.getSource());

        try {
            recording.save(repository);
        }
        catch(SQLException exc) {
            CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
            onDestroyCleanUp();
            return ;
        }

        nm.notify(NOTIFICATION_ID, buildNotification(RECORD_SUCCESS, 0));
        onDestroyCleanUp();
    }

    private void onDestroyCleanUp() {
        resetState();
        try {
            ACRA.getErrorReporter().clearCustomData();
        }
        catch (IllegalStateException ignored) {
        }
    }
}
