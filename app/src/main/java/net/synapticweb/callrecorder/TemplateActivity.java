package net.synapticweb.callrecorder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.synapticweb.callrecorder.settings.SettingsFragment;


public abstract class TemplateActivity extends AppCompatActivity {
    private String settedTheme;
    public static final String LIGHT_THEME = "light_theme";
    public static final String DARK_THEME = "dark_theme";
    abstract protected Fragment createFragment();

    public String getSettedTheme() {
        return settedTheme;
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
}
