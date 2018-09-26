package net.synapticweb.callrecorder;

import android.content.Context;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AppLibrary {
    static final int SQLITE_TRUE = 1;
    static final int SQLITE_FALSE = 0;
    static final int UNKNOWN_TYPE_PHONE_CODE = -1;

    //https://stackoverflow.com/questions/2760995/arraylist-initialization-equivalent-to-array-initialization
    public static final List<PhoneTypeContainer> PHONE_TYPES = new ArrayList<>(Arrays.asList(
            new PhoneTypeContainer(1, "Home"),
            new PhoneTypeContainer(2, "Mobile"),
            new PhoneTypeContainer(3, "Work"),
            new PhoneTypeContainer(-1, "Unknown"),
            new PhoneTypeContainer(7, "Other")
    ));

    //https://stackoverflow.com/questions/3659809/where-am-i-get-country
    //De văzut și https://stackoverflow.com/questions/26971806/unexpected-telephonymanager-getsimcountryiso-behaviour
    @Nullable
    public static String getUserCountry(Context context) {
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

    //https://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
    public static String getDurationHuman(long millis) {
//        long hours = TimeUnit.MILLISECONDS.toHours(millis);
//        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    //https://stackoverflow.com/questions/4605527/converting-pixels-to-dp
    public static int pxFromDp(final Context context, final int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    //semnalizează metodele folosite în ContactsListActivityMain și ContactDetailActivity pentru a gestiona fragmentul
    // care listează recordingurile unui contact
    @interface HandleDetailFragment{}
    //semnalizează metodele folosite în ContactsListActivityMain pentru a gestiona fragmentul care listează contactele
    @interface HandleListFragment {}

}
