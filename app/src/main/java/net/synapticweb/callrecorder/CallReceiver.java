package net.synapticweb.callrecorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;



public class CallReceiver extends BroadcastReceiver {
    private final String TAG = "CallRecorder";
    private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUTGOING = "android.intent.action.NEW_OUTGOING_CALL";
    private Bundle bundle;
    private String state;
    private static String inCall = null;
    private static String outCall = null;
    private static boolean serviceStarted = false;
    private static ComponentName serviceName;

    public CallReceiver()
    {
        super();
    }

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
                    if(!serviceStarted && (inCall != null) )
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
            if(!serviceStarted)
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
}
