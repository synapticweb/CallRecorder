package net.synapticweb.callrecorder;

import android.app.Application;

//Oferă context cînd nu este nicio activitate disponibilă. Are nevoie ca să funcționeze de
// android:name=".CrApp" în AndroidManifest.xml
public class CrApp extends Application {
    private static CrApp instance;

    public CrApp() {
        instance = this;
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

}
