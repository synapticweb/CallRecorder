package net.synapticweb.callrecorder;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

//Oferă context cînd nu este nicio activitate disponibilă. Are nevoie ca să funcționeze de
// android:name=".CrApp" în AndroidManifest.xml
//Servește și ca bibliotecă a aplicației.
public class CrApp extends Application {

    public static final int SQLITE_TRUE = 1;
    public static final int SQLITE_FALSE = 0;
    public static final int UNKNOWN_TYPE_PHONE_CODE = -1;
    //https://stackoverflow.com/questions/2760995/arraylist-initialization-equivalent-to-array-initialization
    public static final List<PhoneTypeContainer> PHONE_TYPES = new ArrayList<>(Arrays.asList(
            new PhoneTypeContainer(1, "Home"),
            new PhoneTypeContainer(2, "Mobile"),
            new PhoneTypeContainer(3, "Work"),
            new PhoneTypeContainer(-1, "Unknown"),
            new PhoneTypeContainer(7, "Other")
    ));
    //http://tools.medialab.sciences-po.fr/iwanthue/
    public static final List<Integer> colorList = new ArrayList<>(Arrays.asList(
            0xFF7b569b,
            0xFFb8ad38,
            0xFF586dd7,
            0xFF45aecf,
            0xFFd9a26a,
            0xFFe26855,
            0xFF8c6d2c,
            0xFFa4572e
    ));
    private static CrApp instance;

    public CrApp() {
        instance = this;
    }

    //https://stackoverflow.com/questions/3659809/where-am-i-get-country
    //De văzut și https://stackoverflow.com/questions/26971806/unexpected-telephonymanager-getsimcountryiso-behaviour
    @Nullable
    public static String getUserCountry(Context context) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
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
    public static String getDurationHuman(long millis, boolean spokenStyle) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        if(spokenStyle) {
            String duration = "";
            if(hours > 0)
                duration += (hours + " hour" + (hours > 1 ? "s" : ""));
            if(minutes > 0)
                duration += ((hours > 0 ? ", " : "") + minutes + " minute" + (minutes > 1 ? "s" : "") );
            if(seconds > 0)
                duration += ((minutes > 0 || hours > 0 ? ", " : "") + seconds + " second" + (seconds > 1 ? "s" : "") );
            return duration;
        }
        else {
            if (hours > 0)
                return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
            else
                return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    public static String getFileSizeHuman(long size) {
        double numUnits =  size / 1024;
        String unit = "KB";
        if(numUnits > 1000) {
            numUnits = (int) size / 1048576;
            unit = "MB";
            double diff = (size - numUnits * 1048576) / 1048576;
            numUnits = numUnits + diff;
            if(numUnits > 1000) {
                numUnits = size / 1099511627776L;
                unit = "GB";
                 diff = (size - numUnits * 1099511627776L) / 1099511627776L;
                numUnits = numUnits + diff;
            }
        }
        return new DecimalFormat("#.#").format(numUnits) + " " + unit;
    }

    //https://stackoverflow.com/questions/4605527/converting-pixels-to-dp
    public static int pxFromDp(final Context context, final int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static Spanned getSpannedText(String text, Html.ImageGetter getter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY, getter, null);
        } else
            return Html.fromHtml(text, getter, null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
//        Shell.Config.verboseLogging(BuildConfig.DEBUG);
    }

    public static CrApp getInstance() {
        return instance;
    }

    public static class PhoneTypeContainer {

        private int typeCode;
        private String typeName;

        PhoneTypeContainer(int code, String name)
        {
            typeCode = code;
            typeName = name;
        }

        @Override
        @NonNull
        public String toString(){
            return typeName;
        }

        public int getTypeCode() {
            return typeCode;
        }

        public void setTypeCode(int typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
    }
}
