package net.synapticweb.callrecorder;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

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
        if(settings.getString(SettingsFragment.APP_THEME, "light_theme").equals("light_theme")) {
            settedTheme = LIGHT_THEME;
            setTheme((this.getClass().getSimpleName().equals("PlayerActivity")) ? R.style.PlayerStyleLight
                    : R.style.AppThemeLight);
        }
        else {
            settedTheme = DARK_THEME;
            setTheme((this.getClass().getSimpleName().equals("PlayerActivity")) ? R.style.PlayerStyleDark
                    : R.style.AppThemeDark);
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
