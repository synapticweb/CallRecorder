package net.synapticweb.callrecorder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.util.Pair;
import android.util.Log;
import android.util.LongSparseArray;

import com.google.i18n.phonenumbers.PhoneNumberUtil;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.PhoneTypeContainer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Contact implements Comparable<Contact>, Parcelable {
    private Long id;
    private String phoneNumber = null;
    private int phoneType = AppLibrary.UNKNOWN_TYPE_PHONE_CODE;
    private String contactName = null;
    private Uri photoUri = null;
    private boolean privateNumber = false;
    private boolean shouldRecord = true;
    private static final String TAG = "CallRecorder";

    public Contact(){
    }

    public Contact(Long id, String phoneNumber, String contactName, String photoUriStr, int phoneTypeCode) {
        setId(id);
        setPhoneNumber(phoneNumber);
        setContactName(contactName);
        setPhotoUri(photoUriStr);
        setPhoneType(phoneTypeCode);
    }

    public static Contact getContactIfNumberInDb(String receivedPhoneNumber, Context context) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        List<Contact> contacts = new ArrayList<>();
        String[] projection = {ContactsContract.Contacts._ID,
                ContactsContract.Contacts.COLUMN_NAME_NUMBER,
                ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME,
                ContactsContract.Contacts.COLUMN_NAME_SHOULD_RECORD};
        Cursor cursor = db.query(
                ContactsContract.Contacts.TABLE_NAME, projection, null, null, null, null, null);

        while(cursor.moveToNext())
        {
            Long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_NUMBER));
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME));
            Boolean shouldRecord = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_SHOULD_RECORD)) == 1;
            Contact contact = new Contact(id, number, contactName, null, AppLibrary.UNKNOWN_TYPE_PHONE_CODE);
            contact.setShouldRecord(shouldRecord);
            contacts.add(contact);
        }
        cursor.close();

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        for (Contact contact : contacts) {
            String dbNumPhone = contact.getPhoneNumber();
            PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(receivedPhoneNumber, dbNumPhone);
            if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER)
                return contact;
        }
        return null;
    }

    public void updateContact(Context context, boolean byNumber) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.COLUMN_NAME_NUMBER, getPhoneNumber());
        values.put(ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME, getContactName());
        values.put(ContactsContract.Contacts.COLUMN_NAME_PHONE_TYPE, getPhoneTypeCode());
        values.put(ContactsContract.Contacts.COLUMN_NAME_PHOTO_URI,
                (getPhotoUri() == null) ? null : getPhotoUri().toString());
        values.put(ContactsContract.Contacts.COLUMN_NAME_SHOULD_RECORD, shouldRecord());
        values.put(ContactsContract.Contacts.COLUMN_NAME_PRIVATE_NUMBER, isPrivateNumber());

        try {
            if(byNumber)
                db.update(ContactsContract.Contacts.TABLE_NAME, values,
                    ContactsContract.Contacts.COLUMN_NAME_NUMBER + "='" + getPhoneNumber() + "'", null);
            else
                db.update(ContactsContract.Contacts.TABLE_NAME, values,
                        ContactsContract.Contacts._ID + "=" + getId(), null);
        }
        catch (SQLException exception) {
            Log.wtf(TAG, exception.getMessage());
        }
    }

    public void copyPhotoIfExternal(Context context) throws IOException {
      if(photoUri != null && !photoUri.getAuthority().equals("net.synapticweb.callrecorder.fileprovider")) {
          Bitmap originalPhotoBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
          File copiedPhotoFile = new File(context.getFilesDir(), getPhoneNumber() + ".jpg");
          OutputStream os = new FileOutputStream(copiedPhotoFile);
          originalPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 70, os);
          setPhotoUri(FileProvider.getUriForFile(context, "net.synapticweb.callrecorder.fileprovider", copiedPhotoFile));
      }
    }

    public void insertInDatabase(Context context) throws SQLException {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(ContactsContract.Contacts.COLUMN_NAME_NUMBER, phoneNumber);
        values.put(ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME, contactName);
        values.put(ContactsContract.Contacts.COLUMN_NAME_PHOTO_URI, photoUri == null ? null : photoUri.toString());
        values.put(ContactsContract.Contacts.COLUMN_NAME_PHONE_TYPE, phoneType);
        values.put(ContactsContract.Contacts.COLUMN_NAME_SHOULD_RECORD, shouldRecord);
        values.put(ContactsContract.Contacts.COLUMN_NAME_PRIVATE_NUMBER, privateNumber);

        setId(db.insertOrThrow(ContactsContract.Contacts.TABLE_NAME, null, values));
    }

    public void delete(Context context) throws SQLException {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        Cursor cursor = db.query(RecordingsContract.Recordings.TABLE_NAME,
                new String[]{RecordingsContract.Recordings._ID, RecordingsContract.Recordings.COLUMN_NAME_PATH},
                RecordingsContract.Recordings.COLUMN_NAME_PHONE_NUM_ID + "=" + getId(), null, null, null, null);

        while(cursor.moveToNext()) {
           Recording recording =
                    new Recording(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings._ID)),
                            cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_PATH)),
                            null, null, null);
           recording.delete(context);
        }

        cursor.close();
        if(getPhotoUri() != null ) //întotdeauna este poza noastră.
            context.getContentResolver().delete(getPhotoUri(), null, null);
        //dacă foloseam nr pentru a identifica contactul crăpa la numerele private
     if((db.delete(ContactsContract.Contacts.TABLE_NAME, ContactsContract.Contacts._ID
                + "=" + getId(), null)) == 0)
         throw new SQLException("This Contacts row was not deleted");
    }

  @Nullable static public Contact searchNumberInPhoneContacts(final String number, @NonNull final Context context) {
        Contact contact = null;
      Cursor cursor = context.getContentResolver()
              .query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{
                              android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                              android.provider.ContactsContract.CommonDataKinds.Phone.TYPE,
                              android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                              android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI},
                      null, null, null);

      if(cursor != null) {
          PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
          while (cursor.moveToNext()) {
              String numberContacts = cursor.getString(
                      cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
              PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(numberContacts, number);
              if(matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                  contact = new Contact();
                  contact.setPhoneType(cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE)));
                  contact.setContactName(cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                  contact.setPhotoUri(cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI)));
                  contact.setPhoneNumber(cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)));
                  break;
              }
          }
          cursor.close();
          return contact;
      }
        return null;
    }

    public boolean shouldRecord() {
        return shouldRecord;
    }

    public void setShouldRecord(boolean shouldRecord) {
        this.shouldRecord = shouldRecord;
    }

    public boolean isPrivateNumber() {
        return privateNumber;
    }

    public void setPrivateNumber(boolean privateNumber) {
        this.privateNumber = privateNumber;
    }


    public int compareTo(@NonNull Contact numberToCompare)
    {
        return this.contactName.compareTo(numberToCompare.getContactName());
    }


    public String getPhoneNumber() {

        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        if(phoneNumber == null)
            this.phoneNumber = "Private number";
        else
            this.phoneNumber = phoneNumber;
    }

    public int getPhoneTypeCode() {
        return phoneType;
    }

    public String getPhoneTypeName(){
        for(PhoneTypeContainer typeContainer : AppLibrary.PHONE_TYPES)
            if(typeContainer.getTypeCode() == this.phoneType)
                return typeContainer.getTypeName();

        return null;
    }

    public void setPhoneType(String phoneType)
    {
        for(PhoneTypeContainer typeContainer : AppLibrary.PHONE_TYPES)
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
        dest.writeByte(this.privateNumber ? (byte) 1 : (byte) 0);
        dest.writeByte(this.shouldRecord ? (byte) 1 : (byte) 0);
    }

    private Contact(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.phoneNumber = in.readString();
        this.phoneType = in.readInt();
        this.contactName = in.readString();
        this.photoUri = in.readParcelable(Uri.class.getClassLoader());
        this.privateNumber = in.readByte() != 0;
        this.shouldRecord = in.readByte() != 0;
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
