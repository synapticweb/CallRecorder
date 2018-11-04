package net.synapticweb.callrecorder;

import android.app.Application;

import com.topjohnwu.superuser.Shell;

//Oferă context cînd nu este nicio activitate disponibilă. Are nevoie ca să funcționeze de
// android:name=".CallRecorderApplication" în AndroidManifest.xml
public class CallRecorderApplication extends Application {
    private static CallRecorderApplication instance;

    public CallRecorderApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR);
        Shell.Config.verboseLogging(BuildConfig.DEBUG);
    }

    public static CallRecorderApplication getInstance() {
        return instance;
    }

}
