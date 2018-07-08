package net.synapticweb.callrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;


class PhoneNumber implements Comparable<PhoneNumber>, Parcelable {
    private Long id;
    private String phoneNumber = null;
    private int phoneType = -1;
    private String contactName = null;
    private Uri photoUri = null;
    private boolean unkownNumber = false;
    private boolean privateNumber = false;
    private boolean shouldRecord = true;
    private static final String TAG = "CallRecorder";

    PhoneNumber(){
    }

    PhoneNumber(Long id, String phoneNumber, String contactName, String photoUriStr, int phoneTypeCode)
    {
        setId(id);
        setPhoneNumber(phoneNumber);
        setContactName(contactName);
        setPhotoUri(photoUriStr);
        setPhoneType(phoneTypeCode);
        setPhoneType(phoneTypeCode);
    }

    public void updateNumber(Context context, boolean byNumber) {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER, getPhoneNumber());
        values.put(ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME, getContactName());
        values.put(ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE, getPhoneTypeCode());
        values.put(ListenedContract.Listened.COLUMN_NAME_PHOTO_URI,
                (getPhotoUri() == null) ? null : getPhotoUri().toString());
        values.put(ListenedContract.Listened.COLUMN_NAME_UNKNOWN_NUMBER, isUnkownNumber());
        values.put(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD, shouldRecord());
        values.put(ListenedContract.Listened.COLUMN_NAME_PRIVATE_NUMBER, isPrivateNumber());

        try {
            if(byNumber)
                db.update(ListenedContract.Listened.TABLE_NAME, values,
                    ListenedContract.Listened.COLUMN_NAME_NUMBER + "='" + getPhoneNumber() + "'", null);
            else
                db.update(ListenedContract.Listened.TABLE_NAME, values,
                        ListenedContract.Listened._ID + "=" + getId(), null);
        }
        catch (SQLException exception) {
            Log.wtf(TAG, exception.getMessage());
        }
    }

    public void insertInDatabase(Context context) throws SQLException
    {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(ListenedContract.Listened.COLUMN_NAME_NUMBER, phoneNumber);
        values.put(ListenedContract.Listened.COLUMN_NAME_CONTACT_NAME, contactName);
        values.put(ListenedContract.Listened.COLUMN_NAME_PHOTO_URI, photoUri == null ? null : photoUri.toString());
        values.put(ListenedContract.Listened.COLUMN_NAME_PHONE_TYPE, phoneType);
        values.put(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD, shouldRecord);
        values.put(ListenedContract.Listened.COLUMN_NAME_UNKNOWN_NUMBER, unkownNumber);
        values.put(ListenedContract.Listened.COLUMN_NAME_PRIVATE_NUMBER, privateNumber);

        setId(db.insertOrThrow(ListenedContract.Listened.TABLE_NAME, null, values));
    }

    public void delete(Context context) throws SQLException
    {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor cursor = db.query(RecordingsContract.Recordings.TABLE_NAME,
                new String[]{RecordingsContract.Recordings._ID, RecordingsContract.Recordings.COLUMN_NAME_PATH},
                RecordingsContract.Recordings.COLUMN_NAME_PHONE_NUM_ID + "=" + getId(), null, null, null, null);

        while(cursor.moveToNext()) {
           Recording recording =
                    new Recording(context, cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings._ID)),
                            cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_PATH)));
           recording.delete();
        }

        cursor.close();

     if((db.delete(ListenedContract.Listened.TABLE_NAME, ListenedContract.Listened.COLUMN_NAME_NUMBER
                + "='" + getPhoneNumber() + "'", null)) == 0)
         throw new SQLException("This Listened row was not deleted");
    }

  @Nullable static public PhoneNumber searchNumberInContacts(String number,  @NonNull Context context)
    {
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
                PhoneNumber phoneNumber = new PhoneNumber();
                phoneNumber.setPhoneType(cursor.getInt(cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)));
                phoneNumber.setContactName(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)));
                phoneNumber.setPhotoUri(cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)));
                phoneNumber.setPhoneNumber(number);

                cursor.close();
                return phoneNumber;
            }
            else
                return null;
        }
            return null;
    }

    public boolean shouldRecord() {
        return shouldRecord;
    }

    public void setShouldRecord(boolean shouldRecord) {
        this.shouldRecord = shouldRecord;
    }


    public boolean isUnkownNumber() {
        return unkownNumber;
    }

    public void setUnkownNumber(boolean unkownNumber) {
        this.unkownNumber = unkownNumber;
    }

    public boolean isPrivateNumber() {
        return privateNumber;
    }

    public void setPrivateNumber(boolean privateNumber) {
        this.privateNumber = privateNumber;
    }


    public int compareTo(@NonNull  PhoneNumber numberToCompare)
    {
        if(this.isUnkownNumber() && !numberToCompare.isUnkownNumber() )
            return -1;
        if(!this.isUnkownNumber() && numberToCompare.isUnkownNumber())
            return 1;

        return this.contactName.compareTo(numberToCompare.getContactName());
    }


    public String getPhoneNumber() {

        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if(phoneNumber == null)
            this.phoneNumber = "Unknown number";
        else
            this.phoneNumber = phoneNumber;
    }

    public int getPhoneTypeCode() {
        return phoneType;
    }

    public String getPhoneTypeName(){
        for(PhoneTypeContainer typeContainer : GlobalConstants.PHONE_TYPES)
            if(typeContainer.getTypeCode() == this.phoneType)
                return typeContainer.getTypeName();

        return null;
    }

    public void setPhoneType(String phoneType)
    {
        for(PhoneTypeContainer typeContainer : GlobalConstants.PHONE_TYPES)
            if(typeContainer.getTypeName().equals(phoneType)) {
                this.phoneType = typeContainer.getTypeCode();
                break;
            }

    }

    public void setPhoneType(int phoneTypeCode) {
        this.phoneType = phoneTypeCode;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        if(contactName == null) {
            if(isPrivateNumber())
                this.contactName = "Private number";
            else
                this.contactName = "Unknown contact";
        }
        else
            this.contactName = contactName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUriStr) {
        if(photoUriStr != null)
            this.photoUri = Uri.parse(photoUriStr);
        else
            this.photoUri = null;
    }

    public void setPhotoUri(Uri photoUri)
    {
        this.photoUri = photoUri;
    }

    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeString(this.phoneNumber);
        dest.writeInt(this.phoneType);
        dest.writeString(this.contactName);
        dest.writeParcelable(this.photoUri, flags);
        dest.writeByte(this.unkownNumber ? (byte) 1 : (byte) 0);
        dest.writeByte(this.privateNumber ? (byte) 1 : (byte) 0);
        dest.writeByte(this.shouldRecord ? (byte) 1 : (byte) 0);
    }

    private PhoneNumber(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.phoneNumber = in.readString();
        this.phoneType = in.readInt();
        this.contactName = in.readString();
        this.photoUri = in.readParcelable(Uri.class.getClassLoader());
        this.unkownNumber = in.readByte() != 0;
        this.privateNumber = in.readByte() != 0;
        this.shouldRecord = in.readByte() != 0;
    }

    public static final Parcelable.Creator<PhoneNumber> CREATOR = new Parcelable.Creator<PhoneNumber>() {
        @Override
        public PhoneNumber createFromParcel(Parcel source) {
            return new PhoneNumber(source);
        }

        @Override
        public PhoneNumber[] newArray(int size) {
            return new PhoneNumber[size];
        }
    };
}
