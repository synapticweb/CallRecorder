package net.synapticweb.callrecorder.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import net.synapticweb.callrecorder.R;

public class SettingsFragment extends PreferenceFragment {
    //aceste valori vor fi dublate în res/xml/preferences.xml
    public static final String AUTOMMATICALLY_RECORD_PRIVATE_CALLS = "auth_record_priv";
    public static final String PARANOID_MODE = "paranoid";
    public static final String APP_THEME = "theme";
    public static final String DELETE_ON_EXPORT = "delete_on_export";
    public static final String SPEAKER_USE = "speaker_use";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference themeOption = findPreference(APP_THEME);
        Preference paranoidMode = findPreference(PARANOID_MODE);
        paranoidMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preference autoPrivCalls = findPreference(AUTOMMATICALLY_RECORD_PRIVATE_CALLS);
                autoPrivCalls.setEnabled(!autoPrivCalls.isEnabled());
                return true;
            }
        });
        themeOption.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getActivity().recreate();
                return true;
            }
        });
    }
}
