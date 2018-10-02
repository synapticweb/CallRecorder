package net.synapticweb.callrecorder;

import android.app.Application;

//Oferă context cînd nu este nicio activitate disponibilă. Are nevoie ca să funcționeze de
// android:name=".CallRecorderApplication" în AndroidManifest.xml
public class CallRecorderApplication extends Application {
    private static CallRecorderApplication instance;

    public CallRecorderApplication() {
        instance = this;
    }

    public static CallRecorderApplication getInstance() {
        return instance;
    }

}
