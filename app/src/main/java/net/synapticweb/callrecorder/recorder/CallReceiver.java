package net.synapticweb.callrecorder.recorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;


public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallRecorder";
    public static final String ARG_NUM_PHONE = "arg_num_phone";
    public static final String ARG_INCOMING = "arg_incoming";
    private final static String NO_INCALL = "no_incall";
    private final static int PIE_NUM_STATERINGING = 2; //de cîte ori este trimisă starea ringing în pie
    //Variabilele astea e musai să fie statice. Nu se poate face nicio asumpție în legătură cu nr de instanțe CallReceiver care
    //vor fi folosite cînd apare vreuna dintre acțiunile care sunt ascultate. De exemplu ringing poate să fie anunțat cu o
    //instanță, apoi offhook și idle cu alte instanțe, pentru aceeași convorbire.
    private static String inCall = NO_INCALL;
    private static int stateRingingCounter = 0; //în pie, ca și în lolipop stările telefonului se trimit de 2 ori.
    // Numai că la ringing prima dată EXTRA_INCOMING_NUMBER este null, a doua oară conține nr de pe care se sună. Dacă e privat
    //și a doua oară e null.
    private static boolean serviceStarted = false; //Fiind statică, dacă se fac 2 apeluri simultan numai primul poate porni
    //serviciul de recording. Dacă nu ar fi statică s-ar putea porni simultan mai multe servicii. Asta e un lucru rău, pentru
    //că de ex. dacă se sună de pe un nr în timp ce se vorbește cu un altul, dacă userul răspunde la al doilea apel primul e pus
    //pe hold. Cînd userul îi închide celui de-al doilea nu se primește nicio stare idle, ceea ce face ca al doilea serviciu să
    //rămînă pornit fără posibilitate de oprire.
    private static boolean incomingOffhookCalled = false;
    private static ComponentName serviceName = null;

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
    // mai întîi preluat numărul, apoi este pornit serviciul (care va afișa doar o notificare, nu va porni înregistrarea).
    // Cînd starea se schimbă în ofhook (adică userul a raspuns la telefon) este apelat RecorderService.onIncomingOfhook() care
    //decide dacă pornește sau nu automat înregistrarea.
    // O problemă este faptul că, pe lolipop și pe pie boradcasturile phone_state sunt trimise de 2 ori:
    // deci 2 ringing-uri, 2 ofhook-uri și 2 idle-uri. De aceea folosesc cîmpul serviceStarted, ca să știu dacă a fost deja pornit serviciul.
    //Și mai este posibil să se vorbească simultan pe 2 numere, ceea ce nu va avea ca efect pornirea serviciului de 2 ori.
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
        Bundle bundle;
        String state;
        String action = intent.getAction();

        if(action != null && action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) ) {

            if((bundle = intent.getExtras()) != null) {
                state = bundle.getString(TelephonyManager.EXTRA_STATE);
                Log.wtf(TAG, intent.getAction() + " " + state);

                //acum serviciul este pornit totdeauna în extra_state_ringing (pentru ca userul să aibă posibilitatea
                // în cazul nr necunoscute să pornească înregistrarea înainte de începerea convorbirii),
                if(state != null && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    inCall = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    stateRingingCounter++;
                    Log.wtf(TAG, "Incoming number: " + inCall);
                    //A se citi: dacă serviciul nu a fost încă pornit și ori inCall conține deja un nr de telefon ori s-au
                    //primit deja 2 stări ringing și inCall e tot null -> deci se sună de pe un nr privat. (Pie)
                    if(!serviceStarted && (inCall != null || stateRingingCounter == PIE_NUM_STATERINGING) ) {
                        Intent intentService = new Intent(context, RecorderService.class);
                        serviceName = intentService.getComponent();
                        intentService.putExtra(ARG_NUM_PHONE, inCall);
                        intentService.putExtra(ARG_INCOMING, true);

                        context.startService(intentService);
                        serviceStarted = true;
                        Log.wtf(TAG, "Service started at incoming call");
                    }
                }

                else if(state != null && state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    //Citește: dacă serviciul este pornit ȘI nu a fost încă apelat onIncomingOffHook
                    // ȘI: ORI nr este null (privat), ORI este diferit
                    // de no-incall (a fost modificat într-un nr. obișnuit), deci este incoming, nu outgoing.
                    if(serviceStarted && !incomingOffhookCalled && (inCall == null || !inCall.equals(NO_INCALL)) ) {
                        Log.wtf(TAG, "RecorderService.onIncomingOfhook() called");
                        RecorderService.onIncomingOfhook();
                        incomingOffhookCalled = true;
                    }
                }

                else if(state != null && state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    if(serviceStarted) {
                        Intent stopIntent = new Intent(context, RecorderService.class);
                        stopIntent.setComponent(serviceName);
                        context.stopService(stopIntent);
                        serviceStarted = false;
                        Log.wtf(TAG, "Service stopped by CallReceiver");
                    }
                    inCall = NO_INCALL;
                    stateRingingCounter = 0;
                    incomingOffhookCalled = false;
                    serviceName = null;
                }
            }
        }
        else if(action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String outCall;

            Log.wtf(TAG, intent.getAction());
            outCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.wtf(TAG, "Outgoing number: " + outCall);

            if(!serviceStarted) {
                Intent intentService = new Intent(context, RecorderService.class);
                serviceName = intentService.getComponent();
                intentService.putExtra(ARG_NUM_PHONE, outCall);
                intentService.putExtra(ARG_INCOMING, false);

                context.startService(intentService);
                serviceStarted = true;
                Log.wtf(TAG, "Service started at outgoing call");
            }

        }

    }

}
