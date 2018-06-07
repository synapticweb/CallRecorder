package net.synapticweb.callrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;
import static net.synapticweb.callrecorder.GlobalConstants.*;

class PhoneNumber implements Comparable<PhoneNumber> {
    private Context context;
    private String phoneNumber;
    private String phoneType;
    private String contactName;
    private Uri photoUri;
    private boolean unknownPhone = false;
    private boolean privateNumber = false;
    public static final int FOUND_CONTACT = 1;
    public static final int NOTFOUND_CONTACT = 0;
    private static final int ERROR = -1;

    //setează flagul unknown din tabelul listened la true sau false, în funcție de primul parametru. Dacă al doilea parametru
    //nu e null, este folosit pe post de cheie pentru a găsi linia din Listened care trebuie modificată.
    public void toggleUnknownFlag(boolean unknown, String oldNumber)
    {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ListenedContract.Listened.COLUMN_NAME_UNKNOWN, unknown ? SQLITE_TRUE : SQLITE_FALSE);
        values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER, getPhoneNumber());

        db.update(ListenedContract.Listened.TABLE_NAME,
                values, ListenedContract.Listened.COLUMN_NAME_NUMBER + "='" +
                        (oldNumber == null ? getPhoneNumber() : oldNumber) + "'", null);
    }

    public void makePrivatePhoneNumber()
    {
        setPrivateNumber(true);
        setPhoneNumber(null);
        setPhoneType(null);
        setContactName(context.getResources().getString(R.string.private_number));
    }

    public void makeUnknownPhoneNumber(String number)
    {
        setUnknownPhone(true);
        setPhoneNumber(number);
        setContactName(context.getResources().getString(R.string.unkown_contact));
        setPhoneType(context.getResources().getString(R.string.unkown_type));
    }

    // Caută un nr de telefon în baza de date cu contacte. Dacă nr este găsit sunt extrași parametrii tip, nume de contact
    // și uri al pozei și sunt setate cîmpurile corespunzătoare din obiect.
    public int searchContactData(String number) {
        Uri phoneLookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        Cursor cursor = context.getContentResolver().
                query(phoneLookupUri,
                        new String[]{ContactsContract.PhoneLookup.TYPE,
                                ContactsContract.PhoneLookup.DISPLAY_NAME,
                                ContactsContract.PhoneLookup.PHOTO_URI},
                        null, null, null);

        if(cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int typeCode = cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE));
                switch (typeCode) {
                    case 1:
                        phoneType = "Home: ";
                        break;
                    case 2:
                        phoneType = "Mobile: ";
                        break;
                    default:
                        phoneType = "Other: ";
                }
                    contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                   String photoUriStr = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI));
                   if(photoUriStr != null)
                       photoUri = Uri.parse(photoUriStr);
                   else
                       photoUri = null;
                    cursor.close();

                    phoneNumber = number;
                    unknownPhone = false;

                return FOUND_CONTACT;
            }
            else
                return NOTFOUND_CONTACT;
        }

        return ERROR;
    }

    PhoneNumber(Context pContext) {
        context = pContext;
    }

    public boolean isPrivateNumber() {
        return privateNumber;
    }

    public void setPrivateNumber(boolean privateNumber) {
        this.privateNumber = privateNumber;
    }


    public int compareTo(@NonNull  PhoneNumber numberToCompare)
    {
        if(this.isUnknownPhone() && !numberToCompare.isUnknownPhone() )
            return -1;
        if(!this.isUnknownPhone() && numberToCompare.isUnknownPhone())
            return 1;

        return this.contactName.compareTo(numberToCompare.getContactName());
    }


    public boolean isUnknownPhone() {
        return unknownPhone;
    }

    public void setUnknownPhone(boolean unknownPhone) {
        this.unknownPhone = unknownPhone;
    }

    public String getPhoneNumber() {

        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String phoneType) {
        this.phoneType = phoneType;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }
}
