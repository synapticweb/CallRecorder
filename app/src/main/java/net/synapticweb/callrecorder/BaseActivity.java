/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.synapticweb.callrecorder.settings.SettingsFragment;


public abstract class BaseActivity extends AppCompatActivity {
    private String settedTheme;
    public static final String LIGHT_THEME = "light_theme";
    public static final String DARK_THEME = "dark_theme";
    abstract protected Fragment createFragment();

    public String getSettedTheme() {
        return settedTheme;
    }

    public enum LayoutType {
        SINGLE_PANE,
        DOUBLE_PANE
    }

    protected void insertFragment(int fragmentId) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(fragmentId);
        if(fragment == null) {
            fragment = createFragment();
            fm.beginTransaction().
                    add(fragmentId, fragment).
                    commit();
        }
    }

    protected void setTheme() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if(settings.getString(SettingsFragment.APP_THEME, LIGHT_THEME).equals(LIGHT_THEME)) {
            settedTheme = LIGHT_THEME;
            setTheme(R.style.AppThemeLight);
        }
        else {
            settedTheme = DARK_THEME;
            setTheme(R.style.AppThemeDark);
        }
    }

    protected void checkIfThemeChanged() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if(!settings.getString(SettingsFragment.APP_THEME, LIGHT_THEME).equals(settedTheme)) {
            setTheme();
            recreate();
        }
    }

    public LayoutType getLayoutType() {
        boolean listBoxExists = findViewById(R.id.contacts_list_fragment_container) != null;
        boolean detailBoxExists = findViewById(R.id.contact_detail_fragment_container) != null;
        if(listBoxExists && detailBoxExists)
            return LayoutType.DOUBLE_PANE;
        else
            return LayoutType.SINGLE_PANE;
    }
}
