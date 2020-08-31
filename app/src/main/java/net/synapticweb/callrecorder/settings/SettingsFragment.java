/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;

import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsFragment extends PreferenceFragmentCompat {
    //aceste valori vor fi dublate în res/xml/preferences.xml
    public static final String APP_THEME = "theme";
    public static final String STORAGE = "storage";
    public static final String STORAGE_PATH = "public_storage_path";
    public static final String SPEAKER_USE = "put_on_speaker";
    public static final String FORMAT = "format";
    public static final String MODE = "mode";
    public static final String ENABLED = "enabled";
    public static final String SOURCE = "source";
    private BaseActivity parentActivity;
    private SharedPreferences preferences;
    private Context context;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = getContext();
    }

    @Override
    public void onResume() {
        super.onResume();
        Preference storagePath = findPreference(STORAGE_PATH);
        ListPreference storage = findPreference(STORAGE);
        manageStoragePathSummary(null, storage, storagePath);
    }

    private void manageStoragePathSummary(String newValue, ListPreference storage, Preference storagePath) {
        String storageValue = newValue == null ? storage.getValue() : newValue;
        if(storageValue.equals("public")) {
            storagePath.setEnabled(true);
            String path = preferences.getString(STORAGE_PATH, null);
            if(path == null) {
                File externalDir = getActivity().getExternalFilesDir(null);
                if(externalDir != null)
                    path = externalDir.getAbsolutePath();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(STORAGE_PATH, path);
                editor.apply();
            }
            storagePath.setSummary(path);
        }
        else {
            storagePath.setEnabled(false);
            storagePath.setSummary(context.getResources().getString(R.string.private_storage));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //cu androidx este nevoie ca dividerul să fie setat explicit.
        RecyclerView recycler = getListView();
        recycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));
        parentActivity = (BaseActivity) getActivity();
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        addPreferencesFromResource(R.xml.preferences);

        Preference themeOption = findPreference(APP_THEME);
        Preference format = findPreference(FORMAT);
        Preference mode = findPreference(MODE);
        Preference storage = findPreference(STORAGE);
        final Preference storagePath = findPreference(STORAGE_PATH);
        Preference source = findPreference(SOURCE);

        storagePath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Content content = new Content();
                content.setOverviewHeading(context.getResources().getString(R.string.choose_recordings_storage));
                StorageChooser.Theme theme = new StorageChooser.Theme(getActivity());
                theme.setScheme(parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                        parentActivity.getResources().getIntArray(R.array.storage_chooser_theme_light) :
                        parentActivity.getResources().getIntArray(R.array.storage_chooser_theme_dark));

                StorageChooser chooser = new StorageChooser.Builder()
                        .withActivity(getActivity())
                        .withFragmentManager(parentActivity.getFragmentManager())
                        .allowCustomPath(true)
                        .setType(StorageChooser.DIRECTORY_CHOOSER)
                        .withMemoryBar(true)
                        .allowAddFolder(true)
                        .showHidden(true)
                        .withContent(content)
                        .setTheme(theme)
                        .build();

                chooser.show();

                chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String path) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(STORAGE_PATH, path);
                        editor.apply();
                        preference.setSummary(path);
                    }
                });
                return true;
            }
        });

        storage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                manageStoragePathSummary((String) newValue, (ListPreference) preference, storagePath);
                return true;
            }
        });

        storage.setSummaryProvider( preference -> ((ListPreference) preference).getEntry());


        themeOption.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
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

        source.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        format.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        mode.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

    }
}
