package net.synapticweb.callrecorder.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import net.synapticweb.callrecorder.R;

public class SettingsFragment extends PreferenceFragment {
    //aceste valori vor fi dublate în res/xml/preferences.xml
    public static final String AUTOMMATICALLY_RECORD_PRIVATE_CALLS = "auth_record_priv";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
