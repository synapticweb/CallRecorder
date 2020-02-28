/*
* Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
*
* You may use, distribute and modify this code only under the conditions
* stated in the SW Call Recorder license. You should have received a copy of the
* SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
*/

package net.synapticweb.callrecorder.recorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.settings.SettingsFragment;


public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallRecorder";
    public static final String ARG_NUM_PHONE = "arg_num_phone";
    public static final String ARG_INCOMING = "arg_incoming";
    private static boolean serviceStarted = false; //Fiind statică, dacă se fac 2 apeluri simultan numai primul poate porni
    //serviciul de recording. Dacă nu ar fi statică s-ar putea porni simultan mai multe servicii. Asta e un lucru rău, pentru
    //că de ex. dacă se sună de pe un nr în timp ce se vorbește cu un altul, dacă userul răspunde la al doilea apel primul e pus
    //pe hold. Cînd userul îi închide celui de-al doilea nu se primește nicio stare idle, ceea ce face ca al doilea serviciu să
    //rămînă pornit fără posibilitate de oprire.
    private static ComponentName serviceName = null;
    private SharedPreferences settings;

    public CallReceiver() {
        super();
        settings = PreferenceManager.getDefaultSharedPreferences(CrApp.getInstance());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle;
        String state;
        String incomingNumber;
        String action = intent.getAction();

        if(action != null && action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) ) {

            if((bundle = intent.getExtras()) != null) {
                state = bundle.getString(TelephonyManager.EXTRA_STATE);
                Log.d(TAG, intent.getAction() + " " + state);

                //acum serviciul este pornit totdeauna în extra_state_ringing (pentru ca userul să aibă posibilitatea
                // în cazul nr necunoscute să pornească înregistrarea înainte de începerea convorbirii),
                if(state != null && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    //în pie+ va fi întotdeauna null. În celelalte versiuni va conține nr, null însemnănd nr privat.
                    incomingNumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    boolean isEnabled = settings.getBoolean(SettingsFragment.ENABLED, true);
                    Log.d(TAG, "Incoming number: " + incomingNumber);
                    if(!serviceStarted && isEnabled) {
                        Intent intentService = new Intent(context, RecorderService.class);
                        serviceName = intentService.getComponent();
                        intentService.putExtra(ARG_NUM_PHONE, incomingNumber);
                        intentService.putExtra(ARG_INCOMING, true);
                        //https://stackoverflow.com/questions/46445265/android-8-0-java-lang-illegalstateexception-not-allowed-to-start-service-inten
                        //Bugul a fost detectat cu ACRA, nu apare pe dispozitivele mele
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            context.startForegroundService(intentService);
                        else
                            context.startService(intentService);
                        serviceStarted = true;
                    }
                }

                else if(state != null && state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    boolean isEnabled = settings.getBoolean(SettingsFragment.ENABLED, true);
                    //dacă serviciul nu e pornit înseamnă că e un apel outgoing.
                    if(!serviceStarted && isEnabled) { //outgoing
                        Intent intentService = new Intent(context, RecorderService.class);
                        serviceName = intentService.getComponent();
                        intentService.putExtra(ARG_INCOMING, false);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            context.startForegroundService(intentService);
                        else
                            context.startService(intentService);
                        serviceStarted = true;
                    }
                }

                else if(state != null && state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    if(serviceStarted) {
                        Intent stopIntent = new Intent(context, RecorderService.class);
                        stopIntent.setComponent(serviceName);
                        context.stopService(stopIntent);
                        serviceStarted = false;
                    }
                    serviceName = null;
                }
            }
        }
    }

}
