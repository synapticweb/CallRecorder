package net.synapticweb.callrecorder;

import android.app.Application;

public class CallRecorderApplication extends Application {
    private static CallRecorderApplication instance;

    public CallRecorderApplication() {
        instance = this;
    }

    public static CallRecorderApplication getInstance() {
        return instance;
    }

}
