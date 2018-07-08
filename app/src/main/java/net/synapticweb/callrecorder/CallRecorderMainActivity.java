package net.synapticweb.callrecorder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.synapticweb.callrecorder.GlobalConstants.*;


public class CallRecorderMainActivity extends AppCompatActivity  {
    private static final String TAG = "CallRecorder";
    private static final int REQUEST_NUMBER = 1;
    ListenedAdapter adapter;

    @Override
    protected void onResume(){
        super.onResume();
        //e necesar să recreem lista în onResume() pentru că prin sincronizarea unui număr necunoscut se modifică obiectele
        //din baza de date.
        adapter.phoneNumbers = this.getPhoneNumbersList();
        adapter.notifyDataSetChanged();
    }

    private List<PhoneNumber> getPhoneNumbersList() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<PhoneNumber> phoneNumbers = new ArrayList<>();

        Cursor cursor = db.
                query(ListenedContract.Listened.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext())
        {
            PhoneNumber phoneNumber = new PhoneNumber();
            String number = cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER));

            if(cursor.getInt(
                    cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_UNKNOWN_NUMBER)) == SQLITE_TRUE) {
                if((phoneNumber = PhoneNumber.searchNumberInContacts(number, this)) != null)
                {
                    phoneNumber.setUnkownNumber(true);
                    phoneNumber.updateNumber(this, true);
                    phoneNumber.setId(cursor.getLong(cursor.getColumnIndex(ListenedContract.Listened._ID)));
                    phoneNumbers.add(phoneNumber);
                    continue;
                }
                else {
                    phoneNumber = new PhoneNumber();
                    phoneNumber.setUnkownNumber(true);
                }
            }

            if(cursor.getInt(
                    cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PRIVATE_NUMBER)) == SQLITE_TRUE)
                phoneNumber.setPrivateNumber(true);

            phoneNumber.setContactName(
                    cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME)));
            phoneNumber.setPhoneNumber(number);
            phoneNumber.setPhotoUri(
                    cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PHOTO_URI)));

            phoneNumber.setPhoneType(
                    cursor.getInt(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE)));
            phoneNumber.setId(cursor.getLong(cursor.getColumnIndex(ListenedContract.Listened._ID)));
            phoneNumber.setShouldRecord(
                    cursor.getInt(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD)) == 1);

            phoneNumbers.add(phoneNumber);
        }

        cursor.close();
        Collections.sort(phoneNumbers);
        return phoneNumbers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RecyclerView listenedPhones;
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call_recorder_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        if(Build.MANUFACTURER.equalsIgnoreCase("huawei"))
            huaweiAlert();

        FloatingActionButton fab = findViewById(R.id.add_numbers);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                Intent pickNumber = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pickNumber, REQUEST_NUMBER);
            }
        });

        Button settings = findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CallRecorderMainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        listenedPhones = findViewById(R.id.listened_phones);
        listenedPhones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListenedAdapter(this.getPhoneNumbersList());
        listenedPhones.setAdapter(adapter);
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
            //după introducerea noului număr trebuie să recreem lista vizibilă de numere din interfața principală:
            adapter.phoneNumbers = this.getPhoneNumbersList();
            adapter.notifyDataSetChanged();
        }
    }

    public class PhoneHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView contactPhoto;
        TextView mContactName;
        TextView mPhoneNumber;
        PhoneNumber number;

        PhoneHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.listened_phone, parent, false));
            itemView.setOnClickListener(this);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            mContactName = itemView.findViewById(R.id.contact_name);
            mPhoneNumber = itemView.findViewById(R.id.phone_number);
        }

        @Override
        public void onClick(View view) {
            Intent detailIntent = new Intent(CallRecorderMainActivity.this, PhoneNumberDetail.class);
            detailIntent.putExtra("phoneNumber", number);
            startActivity(detailIntent);
        }
    }

    class ListenedAdapter extends RecyclerView.Adapter<PhoneHolder> {
        List<PhoneNumber> phoneNumbers;
        ListenedAdapter(List<PhoneNumber> list){
            phoneNumbers = list;
        }

        @Override
        @NonNull
        public PhoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            return new PhoneHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PhoneHolder holder, int position) {
            PhoneNumber phoneNumber = phoneNumbers.get(position);

            if(phoneNumber.getPhotoUri() != null) {
                holder.contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
                holder.contactPhoto.setImageURI(phoneNumber.getPhotoUri());
            }
            else {
                if(phoneNumber.isPrivateNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_yellow);
                else if(phoneNumber.isUnkownNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_red);
                else
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_blue);
            }

            holder.mContactName.setText(phoneNumber.getContactName());
            holder.number = phoneNumber;
            if(!phoneNumber.isPrivateNumber())
                holder.mPhoneNumber.setText(phoneNumber.getPhoneNumber());
        }

        @Override
        public int getItemCount() {
            return phoneNumbers.size();
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
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

