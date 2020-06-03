/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder;

import android.app.Application;
import android.content.Context;
import net.synapticweb.callrecorder.di.AppComponent;
import net.synapticweb.callrecorder.di.DaggerAppComponent;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

//Oferă context cînd nu este nicio activitate disponibilă. Are nevoie ca să funcționeze de
// android:name=".CrApp" în AndroidManifest.xml
//Servește și ca bibliotecă a aplicației.
@AcraCore(reportFormat = StringFormat.KEY_VALUE_LIST)
@AcraHttpSender(uri = "http://crashes.infopsihologia.ro",
        httpMethod = HttpSender.Method.POST )
public class CrApp extends Application {
    public AppComponent appComponent;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if(!BuildConfig.DEBUG)
            ACRA.init(this);
        appComponent = DaggerAppComponent.factory().create(base);
    }

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
