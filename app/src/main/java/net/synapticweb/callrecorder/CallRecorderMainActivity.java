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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CallRecorderMainActivity extends AppCompatActivity  {
    private static String TAG = "CallRecorder";
    private static final int REQUEST_NUMBER = 1;
    private RecyclerView listenedPhones;
    ListenedAdapter adapter;

    @Override
    protected void onResume(){
        super.onResume();
        //e necesar să recreem lista în onResume() pentru că prin sincronizarea unui număr necunoscut se modifică obiectele
        //din baza de date.
        adapter.phoneNumbers = this.getPhoneNumbersList();
        adapter.notifyDataSetChanged();
    }

    private void makeUnknownPhoneNumber(PhoneNumber phoneNumber, Cursor cursor)
    {
        phoneNumber.setUnknownPhone(true);
        phoneNumber.setPhoneNumber(cursor.getString(cursor.
                getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER)));
        phoneNumber.setContactName(getResources().getString(R.string.unkown_contact));
        phoneNumber.setPhoneType(getResources().getString(R.string.unkown_type));
    }

//Această funcție produce o listă sortată de obiecte PhoneNumber care corespunde numerelor
// de telefon stocate în baza de date. Lista va fi folosită pentru a inițializa adapterul recyclerview.
    private List<PhoneNumber> getPhoneNumbersList() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<PhoneNumber> phoneNumbers = new ArrayList<>();

        //mai întîi sunt extrase toate liniile din tabelul listened (numerele ascultate) și se ciclează trecînd prin fiecare:
        Cursor cursor = db.
                query(ListenedContract.Listened.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext())
        {
            PhoneNumber phoneNumber = new PhoneNumber();
            long idNumber = 0;
            String lookupKey = null;
            String phoneNumberListened = null;
            //dacă este un număr necunoscut (care nu corespunde unui contact) cîmpul id în tabelul listened va fi null.
            //În această situație idNumber va rămîne 0.
            if(!cursor.isNull(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER_ID)))
                idNumber = cursor.getLong(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER_ID));
            phoneNumberListened = cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER));

            if(idNumber != 0) {     //doar în cazul în care este un număr cunoscut
                // Pe baza id-ului stocat anterior se interoghează ContactsContract.Data pentru a se obține numărul,
                // tipul de număr și cheia lookup, necesară pentru a interoga ContactsContract.Contacts.
                Cursor cursor2 = getContentResolver().
                        query(ContactsContract.Data.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                        ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY},
                                ContactsContract.Data._ID + "=" + idNumber, null, null);

                if (cursor2 != null) {
                    //Dacă numărul a fost cumva șters interogarea de mai sus va returna 0 rezultate.
                    if(cursor2.getCount() == 0)
                    {
                        //în această situație trebuie să modificăm linia din listened corespunzătoare acestui număr încît să
                        //devină din nou număr necunoscut, adică să setăm number_id la null.
                        ContentValues values = new ContentValues();
                        values.putNull(ListenedContract.Listened.COLUMN_NAME_NUMBER_ID);
                        values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER, //E NEVOIE DE ASTA?
                                cursor.getString(cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER)));

                        db.update(ListenedContract.Listened.TABLE_NAME,
                                values, ListenedContract.Listened.COLUMN_NAME_NUMBER_ID + "=" + idNumber, null);
                        //se construiește un PhoneNumber unknown:
                        this.makeUnknownPhoneNumber(phoneNumber, cursor);
                        phoneNumbers.add(phoneNumber);
                        continue;
                    }
            //Acum populăm cîmpurile obiectului PhoneNumber cu valorile obținute în timp real din tabelele de contacte:
                    cursor2.moveToFirst();
                    phoneNumber.setPhoneNumber(
                            cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    //aici trebuie să verificăm dacă numărul extras din contacte este același cu cel pe care îl avem în
                    //listened; dacă nu, vom updata.
                    if(!phoneNumber.getPhoneNumber().equals(phoneNumberListened))
                    {
                        ContentValues values = new ContentValues();
                        values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER_ID, idNumber);
                        values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER, phoneNumber.getPhoneNumber());
                        db.update(ListenedContract.Listened.TABLE_NAME, values,
                                ListenedContract.Listened.COLUMN_NAME_NUMBER_ID + "=" + idNumber, null);
                    }
                    int typeCode = cursor2.getInt(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    switch (typeCode) {
                        case 1:
                            phoneNumber.setPhoneType(getResources().getString(R.string.home_type_phone));
                            break;
                        case 2:
                            phoneNumber.setPhoneType(getResources().getString(R.string.mobile_type_phone));
                            break;
                        default:
                            phoneNumber.setPhoneType(getResources().getString(R.string.other_type_phone));
                    }
                    lookupKey = cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY));
                    cursor2.close();
                }

                Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
                cursor2 = getContentResolver().
                        query(lookupUri,
                                new String[]{ContactsContract.Contacts.DISPLAY_NAME,
                                        ContactsContract.Contacts.PHOTO_URI},
                                null, null, null);

                if (cursor2 != null) {
                    cursor2.moveToFirst();
                    phoneNumber.
                            setContactName(cursor2.getString(cursor2.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                    String photoUriStr = cursor2.getString(cursor2.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
                    cursor2.close();
                    if (photoUriStr != null)
                        phoneNumber.setPhotoUri(Uri.parse(photoUriStr));
                    else
                        phoneNumber.setPhotoUri(null);
                }
            }
            else { //dacă nu are un id valid atunci este nr unknown sau privat:
                if(phoneNumberListened.equals(GlobalConstants.PRIVATE_CALL_DB_NUMBER))
                {
                    phoneNumber.setPrivateNumber(true);
                    phoneNumber.setPhoneNumber("");
                    phoneNumber.setPhoneType("");
                    phoneNumber.setContactName(getResources().getString(R.string.private_number));
                }
                else //e unknown
                    this.makeUnknownPhoneNumber(phoneNumber, cursor);
            }

            phoneNumbers.add(phoneNumber);
        }

        cursor.close();
        //Mai întîi vor apărea numerele unknown. Apoi celelalte, în oride alfabetică a numelui contactului.
        Collections.sort(phoneNumbers);
        return phoneNumbers;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call_recorder_main);

        if(Build.MANUFACTURER.equalsIgnoreCase("huawei"))
        {
            huaweiAlert();
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


        listenedPhones = findViewById(R.id.listened_phones);
        listenedPhones.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListenedAdapter(this.getPhoneNumbersList());
        listenedPhones.setAdapter(adapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri numberUri;
        long idNewNumber = 0;
        String newNumber = null;

        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        //la selectarea unui număr nou se interoghează ContactsContract.Data și se extrage id-ul numărului și numărul.
        //Id-ul este necesar pentru obținerea ulterioară - in timp real - a datelor relevante despre contact iar numărul
        //trebuie stocat pentru situația în care contactul asociat este șters. În această situație numărul va figura ca unknown.
        if (requestCode == REQUEST_NUMBER && (numberUri = data.getData()) != null) {
                Cursor cursor = getContentResolver().
                        query(numberUri, new String[]{ContactsContract.Data._ID,
                                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                                null, null, null);
                if(cursor != null)
                {
                    cursor.moveToFirst();
                    idNewNumber =  cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
                    newNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    cursor.close();
                }

            RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER_ID, idNewNumber);
            values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER, newNumber);
            //în cazul în care este selectat un număr care există deja în baza de date, acesta trebuie să aibă același
            //id și inserarea va eșua din cauza clauzei unique din baza de date.
            try {
                db.insertOrThrow(ListenedContract.Listened.TABLE_NAME, null, values);
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
        Uri contactPhotoUri;
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
            detailIntent.putExtra("contact_photo_uri", (contactPhotoUri == null) ? null : contactPhotoUri.toString() );
            detailIntent.putExtra("contact_name", contactName);
            detailIntent.putExtra("unknown_phone", unknownPhone);
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

            if(phoneNumber.getPhotoUri() != null)
                holder.contactPhoto.setImageURI(phoneNumber.getPhotoUri());
            else {
                if(phoneNumber.isUnknownPhone())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_red);
                else if(phoneNumber.isPrivateNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_yellow);
                else
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_blue);
            }

            holder.mContactName.setText(phoneNumber.getContactName());
            holder.contactName = phoneNumber.getContactName();
            holder.phoneType = phoneNumber.getPhoneType();
            holder.phoneNumber = phoneNumber.getPhoneNumber();
            holder.unknownPhone = phoneNumber.isUnknownPhone();
            holder.contactPhotoUri = phoneNumber.getPhotoUri();
            holder.mPhoneNumber.setText(phoneNumber.getPhoneType() + phoneNumber.getPhoneNumber());
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

