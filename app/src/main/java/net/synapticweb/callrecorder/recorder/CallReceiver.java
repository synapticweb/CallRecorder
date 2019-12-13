/*
* Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
*
* You may use, distribute and modify this code only under the conditions
* stated in the Synaptic Call Recorder license. You should have received a copy of the
* Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
*/

package net.synapticweb.callrecorder.recorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;


public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallRecorder";
    public static final String ARG_NUM_PHONE = "arg_num_phone";
    public static final String ARG_INCOMING = "arg_incoming";
    private final static String NO_INCOMING_NUMBER = "no_incall";
    private final static int PIE_NUM_STATE_RINGING = 2; //de cîte ori este trimisă starea ringing în pie și mai sus
    //Variabilele astea e musai să fie statice. Nu se poate face nicio asumpție în legătură cu nr de instanțe CallReceiver care
    //vor fi folosite cînd apare vreuna dintre acțiunile care sunt ascultate. De exemplu ringing poate să fie anunțat cu o
    //instanță, apoi offhook și idle cu alte instanțe, pentru aceeași convorbire.
    private static String incomingNumber = NO_INCOMING_NUMBER;
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
    // a ști cînd trebuie pornit serviciul pentru apeluri ingoing folosesc cîmpul incomingNumber care este inițializat
    // la "no-incall" și după starea ringing conține nr care sună. Dacă acest cîmp este la valoarea la care a fost inițializat
    // înseamnă că e un apel outgoing și chiar dacă serviciul nu a fost pornit nu trebuie pornit.
    // În cazul numerelor ascunse incomingNumber este setată pe null.
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
                Log.d(TAG, intent.getAction() + " " + state);

                //acum serviciul este pornit totdeauna în extra_state_ringing (pentru ca userul să aibă posibilitatea
                // în cazul nr necunoscute să pornească înregistrarea înainte de începerea convorbirii),
                if(state != null && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    incomingNumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Log.d(TAG, "Incoming number: " + incomingNumber);
                    stateRingingCounter++;
                    //A se citi: dacă serviciul nu a fost încă pornit ȘI (ORI versiunea android e < Pie ORI s-a primit deja de
                    //2 ori starea RINGING). Efectul acestei expresii este că în Pie nu se pornește serviciul decît după ce se
                    //primesc ambele acțiuni RINGING. În versiunile < Pie ori se primește numai o dată RINGING, ori se primește
                    //de ori (lolipop) dar nr este setat din prima. Deci e ok să-i dăm drumul orice ar fi, fără să mai verificăm
                    //stateRingingCounter. În Pie și mai sus nr apare de abia la al doilea RINGING (după docs nu se poate ști
                    //în care, eu am observat că totdeauna în al doilea).
                    //De asemenea, în Pie dacă nu avem permisiunea READ_CALL_LOG starea RINGING se primește o singură dată
                    //și EXTRA_INCOMING_NUMBER este null.
                    if(!serviceStarted && (Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                            stateRingingCounter == PIE_NUM_STATE_RINGING) ) {
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
                    //Citește: dacă serviciul este pornit ȘI nu a fost încă apelat onIncomingOffHook
                    // ȘI este diferit de no-incall (a fost modificat într-un nr. obișnuit sau în null), deci este incoming, nu outgoing.
                    if(serviceStarted && !incomingOffhookCalled && (incomingNumber == null || !incomingNumber.equals(NO_INCOMING_NUMBER)) ) {
                        RecorderService service = RecorderService.getService();
                        if(service != null)
                            service.onIncomingOfhook();
                        incomingOffhookCalled = true;
                    }
                }

                else if(state != null && state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    if(serviceStarted) {
                        Intent stopIntent = new Intent(context, RecorderService.class);
                        stopIntent.setComponent(serviceName);
                        context.stopService(stopIntent);
                        serviceStarted = false;
                    }
                    incomingNumber = NO_INCOMING_NUMBER;
                    stateRingingCounter = 0;
                    incomingOffhookCalled = false;
                    serviceName = null;
                }
            }
        }
        else if(action != null && action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Log.d(TAG, intent.getAction());
            String outCall;
            outCall = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

            if(!serviceStarted) {
                Intent intentService = new Intent(context, RecorderService.class);
                serviceName = intentService.getComponent();
                intentService.putExtra(ARG_NUM_PHONE, outCall);
                intentService.putExtra(ARG_INCOMING, false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(intentService);
                else
                    context.startService(intentService);
                serviceStarted = true;
            }

        }

    }

}
