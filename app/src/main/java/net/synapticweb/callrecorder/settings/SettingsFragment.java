package net.synapticweb.callrecorder.settings;

import android.os.Bundle;
import android.view.View;

import net.synapticweb.callrecorder.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsFragment extends PreferenceFragmentCompat {
    //aceste valori vor fi dublate în res/xml/preferences.xml
    public static final String AUTOMMATICALLY_RECORD_PRIVATE_CALLS = "auth_record_priv";
    public static final String PARANOID_MODE = "paranoid";
    public static final String APP_THEME = "theme";
    public static final String DELETE_ON_EXPORT = "delete_on_export";
    public static final String SPEAKER_USE = "speaker_use";
    public static final String FORMAT = "format";


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //cu androidx este nevoie ca dividerul să fie setat explicit.
        RecyclerView recycler = getListView();
        recycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        Preference themeOption = findPreference(APP_THEME);
        Preference paranoidMode = findPreference(PARANOID_MODE);
        Preference speakerUse = findPreference(SPEAKER_USE);
        Preference format = findPreference(FORMAT);
        paranoidMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preference autoPrivCalls = findPreference(AUTOMMATICALLY_RECORD_PRIVATE_CALLS);
                autoPrivCalls.setEnabled(!autoPrivCalls.isEnabled());
                return true;
            }
        });

        themeOption.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        speakerUse.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        themeOption.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getActivity().recreate();
                return true;
            }
        });

        format.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

    }
}
