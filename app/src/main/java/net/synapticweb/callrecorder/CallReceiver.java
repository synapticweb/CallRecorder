package net.synapticweb.callrecorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.Locale;


public class CallReceiver extends BroadcastReceiver {
    private final String TAG = "CallRecorder";
    private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUTGOING = "android.intent.action.NEW_OUTGOING_CALL";
    private Bundle bundle;
    private String state;
    private static String inCall = "no-incall";
    private static String outCall = null;
    private static boolean serviceStarted = false;
    private static ComponentName serviceName;

    public CallReceiver()
    {
        super();
    }
    //receiverul receptează 2 acțiuni: android.intent.action.PHONE_STATE (declanșat cînd se schimbă starea de apel a
    // dispozitivului) și android.intent.action.NEW_OUTGOING_CALL (cînd se inițiază un apel outgoing de pe dispozitiv).
    // Cînd starea nouă este RINGING intentul conține un cîmp extra suplimentar: EXTRA_INCOMING_NUMBER, cu care se poate
    //accesa nr care sună. Astfel sunt detectate apelurile ingoing.
    //Apelurile outgoing sunt detectate cu ajutorul acțiunii NEW_OUTGOING_CALL. Intentul va avea un cîmp extra suplimentar
    // care conține nr spre care se sună: EXTRA_PHONE_NUMBER.
    // Apelurile ingoing sunt detectate prin schimbarea stării de apel în "ringing". Cînd se întîmplă asta este
    // mai întîi preluat numărul, apoi se așteaptă schimbarea stării în ofhook (adică userul a raspuns la telefon).
    // Cînd apare starea ofhook trebuie pornit RecorderService. O problemă este faptul că, pe lolipop și posibil pe alte
    //versiuni boradcasturile phone_state sunt trimise de 2 ori: deci 2 ringing-uri și 2 ofhook-uri. De aceea folosesc
    //cîmpul serviceStarted, ca să știu dacă a fost deja pornit serviciul.
    // Starea ofhook apare și la apelurile outgoing, imediat ce începe să sune, nu cînd răspunde celălalt device. Pentru
    // a ști cînd trebuie pornit serviciul pentru apeluri ingoing folosesc cîmpul inCall care este inițializat
    // la "no-incall" și după starea ringing conține nr care sună. Dacă acest cîmp este la valoarea la care a fost inițializat
    // înseamnă că e un apel outgoing și chiar dacă serviciul nu a fost pornit nu trebuie pornit.
    // În cazul numerelor ascunse inCall este setată pe null.
    //RecorderService este oprit cînd starea se schimbă la IDLE. E necesar să se verifice dacă serviciul este pornit fiindcă
    // broadcasturile call_state vin de 2 ori.
    //Anumite probleme cu oprirea serviciului au făcut ca numele componentului intentului să fie salvat la pornirea serviciului
    // La oprire, intentul preia numele de conmponent salvat anterior.
    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.getAction().equals(ACTION_PHONE_STATE) )
        {
            if((bundle = intent.getExtras()) != null)
            {
                state = bundle.getString(TelephonyManager.EXTRA_STATE);
                Log.wtf(TAG, intent.getAction() + " " + state);

                if(state.equals(TelephonyManager.EXTRA_STATE_RINGING))
                {
                    inCall = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

                    Log.wtf(TAG, "Incoming number: " + inCall);
                }
                else if(state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
                {
                    //Citește: dacă serviciul nu a fost deja pornit ȘI: ORI nr este null (privat), ORI este diferit
                    // de no-incall (a fost modificat într-un nr. obișnuit)
                    if(!serviceStarted && (inCall == null || !inCall.equals("no-incall")) )
                    {
                        Intent intentService = new Intent(context, RecorderService.class);
                        serviceName = intentService.getComponent();
                        intentService.putExtra("phoneNumber", inCall);
                        intentService.putExtra("incoming", true);

                        context.startService(intentService);
                        serviceStarted = true;
                        Log.wtf(TAG, "Service started at incoming call");
                    }
                }

                else if(state.equals(TelephonyManager.EXTRA_STATE_IDLE))
                {
                    if(serviceStarted) {
                        Intent stopIntent = new Intent(context, RecorderService.class);
                        stopIntent.setComponent(serviceName);
                        context.stopService(stopIntent);
                        serviceStarted = false;
                        Log.wtf(TAG, "Service stopped");
                    }
                }
            }
        }
        else if(intent.getAction().equals(ACTION_OUTGOING))
        {
            Log.wtf(TAG, intent.getAction());
            outCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.wtf(TAG, "Outgoing number: " + outCall);
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String countryCode = getUserCountry(context);
            if(countryCode == null)
                countryCode = "US";

            if(!serviceStarted && phoneUtil.isPossibleNumber(outCall, countryCode))
            {
                Intent intentService = new Intent(context, RecorderService.class);
                serviceName = intentService.getComponent();
                intentService.putExtra("phoneNumber", outCall);
                intentService.putExtra("incoming", false);

                context.startService(intentService);
                serviceStarted = true;
                Log.wtf(TAG, "Service started at outgoing call");
            }

        }

    }

    //https://stackoverflow.com/questions/3659809/where-am-i-get-country
    @Nullable
    private String getUserCountry(Context context) {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if(tm != null) {
                final String simCountry = tm.getSimCountryIso();
                if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                    return simCountry.toUpperCase(Locale.US);
                } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                    String networkCountry = tm.getNetworkCountryIso();
                    if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                        return networkCountry.toUpperCase(Locale.US);
                    }
                }
            }
        return null;
    }

}
