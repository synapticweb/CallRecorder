/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.HelpActivity;
import net.synapticweb.callrecorder.settings.SettingsActivity;
import net.synapticweb.callrecorder.setup.SetupActivity;


public class ContactsListActivityMain extends BaseActivity {
    private static final int SETUP_ACTIVITY = 3;
    public static final String HAS_ACCEPTED_EULA = "has_accepted_eula";
    public static final int EULA_NOT_ACCEPTED = 1;
    public static final int PERMS_NOT_GRANTED = 2;
    public static final int POWER_OPTIMIZED = 4;
    public static final String SETUP_ARGUMENT = "setup_arg";

    @Override
    protected Fragment createFragment() {
        return new ContactsListFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfThemeChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masterdetail);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        TextView title = findViewById(R.id.actionbar_title);
        title.setText(getString(R.string.app_name));
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        int eulaNotAccepted = settings.getBoolean(HAS_ACCEPTED_EULA, false) ? 0 : EULA_NOT_ACCEPTED;
        int permsNotGranted = 0, powerOptimized = 0;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permsNotGranted = checkPermissions() ? 0 : PERMS_NOT_GRANTED;
            if(pm != null)
                powerOptimized = pm.isIgnoringBatteryOptimizations(getPackageName()) ? 0 : POWER_OPTIMIZED;
        }
        int checkResult = eulaNotAccepted | permsNotGranted | powerOptimized;

        if(checkResult != 0) {
            Intent setupIntent = new Intent(this, SetupActivity.class);
            setupIntent.putExtra(SETUP_ARGUMENT, checkResult);
            startActivityForResult(setupIntent, SETUP_ACTIVITY);
        }
        //https://www.truiton.com/2017/01/android-bottom-navigation-bar-example/
        //https://guides.codepath.com/android/Bottom-Navigation-Views
        BottomNavigationView bottomNav = findViewById(R.id.bottom_tab_nav);
        bottomNav.setOnNavigationItemSelectedListener( (@NonNull MenuItem item) -> {
                FragmentManager fm = getSupportFragmentManager();

                switch (item.getItemId()) {
                    case R.id.bottom_nav_contacts:
                        Fragment listContacts = createFragment();
                        resetActionBar(BottomNavTabs.CONTACTS);

                        if(getLayoutType() == LayoutType.DOUBLE_PANE) {
                            Fragment unassignedToRemove = fm.findFragmentById(R.id.tab_fragment_container);
                            if (unassignedToRemove != null) 
                                fm.beginTransaction().remove(unassignedToRemove).commit();
                        }
                        
                        fm.beginTransaction().replace(R.id.contacts_list_fragment_container, listContacts)
                                    .commit();
                        break;

                    case R.id.bottom_nav_unassigned:
                        Fragment unassignedToInsert = new UnassignedRecordingsFragment();
                        resetActionBar(BottomNavTabs.UNASSIGNED);
                        if(getLayoutType() == LayoutType.SINGLE_PANE) {
                            fm.beginTransaction().replace(R.id.contacts_list_fragment_container, unassignedToInsert)
                                    .commit();
                        }
                        else {
                            Fragment oldListcontacts = fm.findFragmentById(R.id.contacts_list_fragment_container);
                            Fragment oldDetail = fm.findFragmentById(R.id.contact_detail_fragment_container);
                            if (oldListcontacts != null)
                                fm.beginTransaction().remove(oldListcontacts).commit();
                            if (oldDetail != null) 
                                fm.beginTransaction().remove(oldDetail).commit();
                            
                            fm.beginTransaction().add(R.id.tab_fragment_container, unassignedToInsert)
                                    .commit();
                        }
                }
                return true;
            });

        //În tablet view, unassigned tab, dacă erau recordinguri selectate și se răsturna ecranul în actionbar
        //apăreau butoanele din tabul Contacts. Se întîmpla asta pentru că metoda toggleSelectModeActionBar
        //era apelată de 2 ori: prima dată din UnassignedRecordingsFragment, a doua oară din
        //ContactDetailFragment. Problema este că la restartarea activității se apelează totdeauna insertFragment
        //Condiția de mai jos repară (cumva) acest bug.
        if(savedInstanceState == null)
            insertFragment(R.id.contacts_list_fragment_container);

        ImageButton hamburger = findViewById(R.id.hamburger);
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        int navWidth;
        int pixelsDp = (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        navWidth = (pixelsDp >= 480) ? (int) (getResources().getDisplayMetrics().widthPixels * 0.4) :
                (int) (getResources().getDisplayMetrics().widthPixels * 0.8);

        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) navigationView.getLayoutParams();
        params.width = navWidth;
        navigationView.setLayoutParams(params);

        hamburger.setOnClickListener( (View v) -> drawer.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener( (@NonNull MenuItem item) -> {
                switch (item.getItemId()) {
                    case R.id.settings: startActivity(new Intent(ContactsListActivityMain.this, SettingsActivity.class));
                        break;
                    case R.id.help: /*Crashlytics.getInstance().crash();*/ startActivity(new Intent(ContactsListActivityMain.this, HelpActivity.class));
                        break;
                    case R.id.rate_app:
                        //https://stackoverflow.com/questions/10816757/rate-this-app-link-in-google-play-store-app-on-the-phone
                      //String packageName = "net.synapticweb.callrecorder.gpcompliant.full";
                        Uri uri = Uri.parse("market://details?id=" + getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        try {
                            startActivity(goToMarket);
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                        }
                }
                drawer.closeDrawers();
                return true;
            });
    }
    
    enum BottomNavTabs { CONTACTS, UNASSIGNED }

    private void resetActionBar(BottomNavTabs tab) {
        ImageButton hamburger =findViewById(R.id.hamburger);
        ImageButton closeBtn = findViewById(R.id.close_select_mode);
        ImageButton editBtn = findViewById(R.id.edit_contact);
        ImageButton callBtn = findViewById(R.id.call_contact);
        ImageButton moveBtn = findViewById(R.id.actionbar_select_move);
        ImageButton selectAllBtn = findViewById(R.id.actionbar_select_all);
        ImageButton infoBtn = findViewById(R.id.actionbar_info);
        ImageButton menuRightBtn = findViewById(R.id.contact_detail_menu);
        ImageButton menuRightSelectedBtn = findViewById(R.id.contact_detail_selected_menu);
        TextView actionBarTitle = findViewById(R.id.actionbar_title);

        hamburger.setVisibility(View.VISIBLE);
        hamburger.setAlpha(1f);
        closeBtn.setVisibility(View.GONE);
        moveBtn.setVisibility(View.GONE);
        selectAllBtn.setVisibility(View.GONE);
        infoBtn.setVisibility(View.GONE);
        menuRightSelectedBtn.setVisibility(View.GONE);
        Toolbar.LayoutParams params = (Toolbar.LayoutParams) actionBarTitle.getLayoutParams();
        params.gravity = Gravity.CENTER;
        actionBarTitle.setLayoutParams(params);
        actionBarTitle.setText(getResources().getString(R.string.app_name));

        if(getLayoutType() == LayoutType.DOUBLE_PANE) {
            if (tab == BottomNavTabs.CONTACTS) {
                editBtn.setVisibility(View.VISIBLE);
                callBtn.setVisibility(View.VISIBLE);
                menuRightBtn.setVisibility(View.VISIBLE);
            } else {
                editBtn.setVisibility(View.GONE);
                callBtn.setVisibility(View.GONE);
                menuRightBtn.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //necesar pentru că altfel nu apelează onActivityResult din fragmente:
        // https://stackoverflow.com/questions/6147884/onactivityresult-is-not-being-called-in-fragment
        if(resultCode == RESULT_OK && requestCode == SETUP_ACTIVITY) {
            if(data.getBooleanExtra(SetupActivity.EXIT_APP, true))
                finish();
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .title(R.string.exit_app_title)
                .icon(getResources().getDrawable(R.drawable.question_mark))
                .content(R.string.exit_app_message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive((@NonNull MaterialDialog dialog, @NonNull DialogAction which) ->
                        ContactsListActivityMain.super.onBackPressed())
                .show();
    }

    private boolean checkPermissions() {
        boolean phoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
        boolean recordAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        boolean readContacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
        boolean readStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        boolean writeStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        return phoneState && recordAudio && readContacts && readStorage && writeStorage;
    }

}