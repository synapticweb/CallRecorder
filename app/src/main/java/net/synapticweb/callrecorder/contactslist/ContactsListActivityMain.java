package net.synapticweb.callrecorder.contactslist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;

import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.os.Bundle;


import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.topjohnwu.superuser.Shell;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.settings.SettingsActivity;
import net.synapticweb.callrecorder.settings.SettingsFragment;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


public class ContactsListActivityMain extends TemplateActivity {
    private static final String TAG = "CallRecorder";
    private static final int PERMISSION_REQUEST = 2;
    private static final int REQUEST_NUMBER = 1;
    private static final String MAKE_SYSTEM_APP = "make_system";
    private static final String MAKE_NORMAL_APP = "make_normal";

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

        insertFragment(R.id.contacts_list_fragment_container);

        if(Build.MANUFACTURER.equalsIgnoreCase("huawei"))
            huaweiAlert();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            askForPermissions();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Log.wtf(TAG, "" + ContextCompat.checkSelfPermission(this, Manifest.permission.CAPTURE_AUDIO_OUTPUT));

        FloatingActionButton fab = findViewById(R.id.add_numbers);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                Intent pickNumber = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pickNumber, REQUEST_NUMBER);
            }
        });

        Button hamburger = findViewById(R.id.hamburger);
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
//        navigationView.getMenu().findItem(R.id.make_system).setTitle(getResources().getString(R.string.make_system_title,
//                ((getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 1) ? "normal" : "system"));


        hamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.settings:
                        Intent intent = new Intent(ContactsListActivityMain.this, SettingsActivity.class);
                        startActivity(intent);
                        break;
//                    case R.id.make_system:
//                        toggleSystem();
//                        break;
                }
                drawer.closeDrawers();
                return true;
            }
        });
    }

    private void toggleSystem() {
        new MaterialDialog.Builder(this)
                .title("Make system app")
                .content(R.string.make_system)
                .positiveText("Make CallRecorder system app")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String direction = ((getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 1) ? MAKE_NORMAL_APP : MAKE_SYSTEM_APP;
                        new InstallSystemAsync(ContactsListActivityMain.this, direction).execute();
                    }
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data); //necesar pentru că altfel nu apelează onActivityResult din fragmente:
        // https://stackoverflow.com/questions/6147884/onactivityresult-is-not-being-called-in-fragment
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .title("Confirm")
                .icon(getResources().getDrawable(R.drawable.question_mark))
                .content("Exit the application?")
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        ContactsListActivityMain.super.onBackPressed();
                    }
                })
                .show();
    }

    private void askForPermissions() {
        boolean outgoingCalls = ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS)
                == PackageManager.PERMISSION_GRANTED;
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

        if(!(outgoingCalls && phoneState && recordAudio && readContacts && readStorage && writeStorage))
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.PROCESS_OUTGOING_CALLS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        boolean notGranted = false;
        if(requestCode == PERMISSION_REQUEST) {
            if(grantResults.length == 0)
                notGranted = true;
            else
            {
                for(int result : grantResults)
                    if(result != PackageManager.PERMISSION_GRANTED) {
                        notGranted = true;
                        break;
                    }
            }
            if(notGranted) {
                new MaterialDialog.Builder(this)
                        .title("Warning")
                        .content("The app was not granted the necessary permissions for proper functioning. " +
                                "As a result, some or all of the app functionality will be lost.")
                        .neutralText(android.R.string.ok)
                        .icon(getResources().getDrawable(R.drawable.warning))
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                            }
                        })
                        .show();
            }
        }
    }

    private static class InstallSystemAsync extends AsyncTask<Void, Void, Void> {
        private WeakReference<ContactsListActivityMain> activityReference;
        private String direction;

        InstallSystemAsync(Context context, String direction) {
            activityReference = new WeakReference<>( (ContactsListActivityMain) context);
            this.direction = direction;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final String MAKE_SYSTEM_SCRIPT = "mount -o rw,remount /system;\n" +
                    "mkdir /system/priv-app/CallRecorder;\n" +
                    "cp  /data/app/net.synapticweb.callrecorder-*/base.apk /system/priv-app/CallRecorder/CallRecorder.apk;\n" +
                    "cp /data/app/net.synapticweb.callrecorder-*/split_lib*  /system/priv-app/CallRecorder;\n" +
                    "cp -r /data/app/net.synapticweb.callrecorder-*/oat /system/priv-app/CallRecorder;\n" +
                    "mv /system/priv-app/CallRecorder/oat/x86/base.odex /system/priv-app/CallRecorder/oat/x86/CallRecorder.odex;\n" +
//                    "mv /system/priv-app/CallRecorder/oat/x86/base.vdex /system/priv-app/CallRecorder/oat/x86/CallRecorder.vdex" +
                    "chown -R root:root /system/priv-app/CallRecorder;\n" +
                    "chmod 0755 /system/priv-app/CallRecorder /system/priv-app/CallRecorder/oat /system/priv-app/CallRecorder/oat/x86;\n" +
                    "chmod 0644 /system/priv-app/CallRecorder/*.apk /system/priv-app/CallRecorder/oat/x86/CallRecorder.odex;\n" +
                    "rm -rf  /data/app/net.synapticweb.callrecorder-*\n";
            final String MAKE_NORMAL_SCRIPT = "mount -o rw,remount /system;\n" +
                    "mkdir /data/app/net.synapticweb.callrecorder-1;\n" +
                    "cp /system/priv-app/CallRecorder/CallRecorder.apk /data/app/net.synapticweb.callrecorder-1/base.apk;\n" +
                    "cp /system/priv-app/CallRecorder/split_lib* /data/app/net.synapticweb.callrecorder-1;\n" +
                    "cp -r /system/priv-app/CallRecorder/oat /data/app/net.synapticweb.callrecorder-1;\n" +
                    "mv /data/app/net.synapticweb.callrecorder-1/oat/x86/CallRecorder.odex /data/app/net.synapticweb.callrecorder-1/oat/x86/base.odex;\n" +
                    "mkdir /data/app/net.synapticweb.callrecorder-1/lib;\n" +
                    "chown -R system:system /data/app/net.synapticweb.callrecorder-1;\n" +
                    "chown -R system:install /data/app/net.synapticweb.callrecorder-1/oat;\n" +
                    "chown system:u0_a29999 /data/app/net.synapticweb.callrecorder-1/oat/x86/base.odex;\n" +
                    "chmod 0755 /data/app/net.synapticweb.callrecorder-1 /data/app/net.synapticweb.callrecorder-1/lib;\n" +
                    "chmod 0771 /data/app/net.synapticweb.callrecorder-1/oat /data/app/net.synapticweb.callrecorder-1/oat/x86;\n" +
                    "chmod 0644 /data/app/net.synapticweb.callrecorder-1/*.apk /data/app/net.synapticweb.callrecorder-1/oat/x86/base.odex;\n" +
                    "rm -rf /system/priv-app/CallRecorder";

            final boolean isRooted = Shell.rootAccess();
            Log.wtf(TAG, "Rooted: " + isRooted);
            if(!isRooted)
                activityReference.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new MaterialDialog.Builder(activityReference.get())
                                .title("Device not rooted")
                                .icon(activityReference.get().getResources().getDrawable(R.drawable.error))
                                .content("This device is not rooted or the su executable is not available.")
                                .neutralText(android.R.string.ok)
                                .show();
                    }
                });
            else {
                String script = (direction.equals(MAKE_NORMAL_APP) ? MAKE_NORMAL_SCRIPT : MAKE_SYSTEM_SCRIPT);
                List<String> output = Shell.su(script).exec().getOut();
                for(String s : output)
                    Log.wtf(TAG, s);
            }

            return null;
        }

    }

    private void huaweiAlert() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final String saveIfSkip = "skipProtectedAppsMessage";
        boolean skipMessage = settings.getBoolean(saveIfSkip, false);
        if (!skipMessage) {
            final SharedPreferences.Editor editor = settings.edit();
            Intent intent = new Intent();
        intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            if (isCallable(intent)) {
                new MaterialDialog.Builder(this)
                        .title("Huawei Protected Apps")
                        .icon(getResources().getDrawable(R.drawable.warning))
                        .content(String.format("%s requires to be enabled in 'Protected Apps' to function properly.%n",
                                getString(R.string.app_name)))
                        .checkBoxPrompt("Do not show again", false, new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                editor.putBoolean(saveIfSkip, isChecked);
                                editor.apply();
                            }
                        })
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                huaweiProtectedApps();
                            }
                        })
                        .show();

            } else {
                editor.putBoolean(saveIfSkip, true);
                editor.apply();
            }
        }
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void huaweiProtectedApps() {
        try {
            String cmd = "am start -n com.huawei.systemmanager/.optimize.process.ProtectActivity";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                cmd += " --user " + getUserSerial();
            }
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ignored) {
        }
    }

    private String getUserSerial() {
        //noinspection ResourceType
        Object userManager = getSystemService(Context.USER_SERVICE);
        if (null == userManager) return "";

        try {
            Method myUserHandleMethod = android.os.Process.class.getMethod("myUserHandle", (Class<?>[]) null);
            Object myUserHandle = myUserHandleMethod.invoke(android.os.Process.class, (Object[]) null);
            Method getSerialNumberForUser = userManager.getClass().getMethod("getSerialNumberForUser", myUserHandle.getClass());
            Long userSerial = (Long) getSerialNumberForUser.invoke(userManager, myUserHandle);
            if (userSerial != null) {
                return String.valueOf(userSerial);
            } else {
                return "";
            }
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored) {
        }
        return "";
    }

}