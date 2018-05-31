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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;



public class CallRecorderMainActivity extends AppCompatActivity  {
    private static String TAG = "CallRecorder";
    private static final int REQUEST_NUMBER = 1;
    private RecyclerView listenedPhones;
    ListenedAdapter adapter;
    private RecorderService recorderService;
    private boolean bound = false;


    @Override
    protected void onResume(){
        super.onResume();
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        adapter.listenedCursor = db.
                query(ListenedContract.Listened.TABLE_NAME, null, null, null, null, null,
                        ListenedContract.Listened.COLUMN_NAME_UNKNOWN_PHONE + " DESC, " +
                                ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME + ", " +
                                ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_call_recorder_main);

        if(Build.MANUFACTURER.equalsIgnoreCase("huawei"))
        {
            huaweiAlert();
            KAService.setServiceAlarm(this);
        }

        FloatingActionButton fab = findViewById(R.id.add_numbers);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                Intent pickNumber = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pickNumber, REQUEST_NUMBER);
            }
        });

        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db
                .query(ListenedContract.Listened.TABLE_NAME, null, null, null, null, null,
                        ListenedContract.Listened.COLUMN_NAME_UNKNOWN_PHONE + " DESC, " +
                                ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME + ", " + ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE);

        listenedPhones = findViewById(R.id.listened_phones);
        listenedPhones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListenedAdapter(cursor);
        listenedPhones.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri numberUri;
        String phoneNumber = null;
        String lookupKey = null;
        String type = null;
        String contactName = null;
        String photoUri = null;

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_NUMBER && (numberUri = data.getData()) != null) {
                Cursor cursor = getContentResolver().
                        query(numberUri, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                        ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY},
                                null, null, null);
                if(cursor != null)
                {
                    cursor.moveToFirst();
                    phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    lookupKey =  cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY));
                    int typeCode = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    switch (typeCode)
                    {
                        case 1:type = "Home: ";
                            break;
                        case 2:type = "Mobile: ";
                            break;
                        default: type = "Other phone type: ";
                    }
                    cursor.close();
                }

                cursor = getContentResolver().
                        query(ContactsContract.Contacts.CONTENT_URI,
                                new String[]{ContactsContract.Contacts.DISPLAY_NAME,
                                        ContactsContract.Contacts.PHOTO_URI},
                                ContactsContract.Contacts.LOOKUP_KEY + "='" + lookupKey + "'", null, null);

                if(cursor != null)
                {
                    cursor.moveToFirst();
                    contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
                    cursor.close();
                }

            RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(ListenedContract.Listened.COLUMN_NAME_UNKNOWN_PHONE, 0);
            values.put(ListenedContract.Listened.COLUMN_NAME_CONTACT_PHOTO_URI, photoUri);
            values.put(ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE, type);
            values.put(ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME, contactName);
            values.put(ListenedContract.Listened.COLUMN_NAME_PHONE_NUMBER, phoneNumber);

            try {
                db.insertOrThrow(ListenedContract.Listened.TABLE_NAME, null, values);
            }
            catch (SQLException exc)
            {
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

            cursor = db.
                query(ListenedContract.Listened.TABLE_NAME, null, null, null, null, null,
                        ListenedContract.Listened.COLUMN_NAME_UNKNOWN_PHONE + " DESC, " +
                                ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME + ", " +
                                ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE);

            adapter.listenedCursor = cursor;
            adapter.notifyDataSetChanged();
        }
    }

    public class PhoneHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView contactPhoto;
        String contactPhotoUri;
        TextView mContactName;
        String contactName;
        TextView mPhoneNumber;
        String phoneNumber;
        String phoneType;
        boolean unknownPhone;

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
            detailIntent.putExtra("phone_number", phoneNumber);
            detailIntent.putExtra("phone_type", phoneType);
            detailIntent.putExtra("contact_photo_uri", contactPhotoUri);
            detailIntent.putExtra("contact_name", contactName);
            detailIntent.putExtra("unknown_phone", unknownPhone);
            startActivity(detailIntent);
        }
    }

    class ListenedAdapter extends RecyclerView.Adapter<PhoneHolder> {
        Cursor listenedCursor;

        ListenedAdapter(Cursor cursor){
            listenedCursor = cursor;
        }

        @Override
        @NonNull
        public PhoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            return new PhoneHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PhoneHolder holder, int position) {
            listenedCursor.moveToPosition(position);

            String phoneNumber = listenedCursor
                    .getString(listenedCursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PHONE_NUMBER));
            String type = listenedCursor
                    .getString(listenedCursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE));
            String contactName = listenedCursor
                    .getString(listenedCursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME));
            String photoUriStr = listenedCursor
                    .getString(listenedCursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_CONTACT_PHOTO_URI));
            boolean unknownPhone = (listenedCursor
                    .getInt(listenedCursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_UNKNOWN_PHONE)) == 1);

            Uri photoUri = null;

            if(photoUriStr != null)
                photoUri = Uri.parse(photoUriStr);

            if(photoUri != null)
                holder.contactPhoto.setImageURI(photoUri);
            else {
                holder.contactPhotoUri = null;
                if(unknownPhone)
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_red);
                else
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_blue);
            }
            holder.mContactName.setText(contactName);
            holder.contactName = contactName;
            holder.phoneType = type;
            holder.phoneNumber = phoneNumber;
            holder.unknownPhone = unknownPhone;
            holder.contactPhotoUri = photoUriStr;
            holder.mPhoneNumber.setText(type + phoneNumber);
        }

        @Override
        public int getItemCount() {
            return listenedCursor.getCount();
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

