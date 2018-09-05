package net.synapticweb.callrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static net.synapticweb.callrecorder.AppLibrary.*;


public class ListContactsActivityMain extends AppCompatActivity  {
    private static final String TAG = "CallRecorder";
    private static final int PERMISSION_REQUEST = 2;
    private static final int REQUEST_NUMBER = 1;

    @Override
    protected void onResume(){
        super.onResume();
        //e necesar să recreem lista în onResume() pentru că prin sincronizarea unui număr necunoscut se modifică obiectele
        //din baza de date.
        FragmentManager fm = getSupportFragmentManager();
        ListContactsFragment contactList = (ListContactsFragment) fm.findFragmentById(R.id.contacts_list_fragment_container);
        if(contactList != null)
            contactList.updateContactsList();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_contacts_activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        if(Build.MANUFACTURER.equalsIgnoreCase("huawei"))
            huaweiAlert();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            askForPermissions();

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
                        Intent intent = new Intent(ListContactsActivityMain.this, SettingsActivity.class);
                        startActivity(intent);
                        break;
                }
                drawer.closeDrawers();
                return true;
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        Fragment contactList = fm.findFragmentById(R.id.contacts_list_fragment_container);
        if(contactList == null) {
            contactList = new ListContactsFragment();
            fm.beginTransaction().
                    add(R.id.contacts_list_fragment_container, contactList).
                    commit();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri numberUri;
        String newNumber = null;
        String contactName = null;
        String photoUri = null;
        int phoneType = UNKNOWN_TYPE_PHONE_CODE;

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_NUMBER && (numberUri = data.getData()) != null) {
                Cursor cursor = getContentResolver().
                        query(numberUri, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                                        ContactsContract.CommonDataKinds.Phone.TYPE},
                                null, null, null);
                if(cursor != null)
                {
                    cursor.moveToFirst();
                    newNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                    phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    cursor.close();
                }


            PhoneNumber phoneNumber = new PhoneNumber(null, newNumber, contactName, photoUri, phoneType);
            try {
                phoneNumber.insertInDatabase(this);
            }
            catch (SQLException exc) {
                if(exc.toString().contains("UNIQUE"))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.number_exists_message)
                            .setTitle(R.string.number_exists_title)
                            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            }
        );
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
            FragmentManager fm = getSupportFragmentManager();
            ListContactsFragment contactList = (ListContactsFragment) fm.findFragmentById(R.id.contacts_list_fragment_container);
            //după introducerea noului număr trebuie să recreem lista vizibilă de numere din interfața principală:
            if(contactList != null)
                contactList.updateContactsList();
        }
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
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The app was not granted the necessary permissions for proper functioning. " +
                        "As a result, some or all of the app functionality will be lost.")
                        .setTitle("Warning")
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                }
                        );
                AlertDialog dialog = builder.create();
                dialog.show();
            }

        }
    }

    private void huaweiAlert() {
        final SharedPreferences settings = getSharedPreferences("ProtectedApps", MODE_PRIVATE);
        final String saveIfSkip = "skipProtectedAppsMessage";
        boolean skipMessage = settings.getBoolean(saveIfSkip, false);
        if (!skipMessage) {
            final SharedPreferences.Editor editor = settings.edit();
            Intent intent = new Intent();
        intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            if (isCallable(intent)) {
                final AppCompatCheckBox dontShowAgain = new AppCompatCheckBox(this);
                dontShowAgain.setText("Do not show again");
                dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        editor.putBoolean(saveIfSkip, isChecked);
                        editor.apply();
                    }
                });

                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Huawei Protected Apps")
                        .setMessage(String.format("%s requires to be enabled in 'Protected Apps' to function properly.%n", getString(R.string.app_name)))
                        .setView(dontShowAgain)
                        .setPositiveButton("Protected Apps", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                huaweiProtectedApps();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
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

