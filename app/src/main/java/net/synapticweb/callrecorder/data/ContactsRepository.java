package net.synapticweb.callrecorder.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.synapticweb.callrecorder.CallRecorderApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.synapticweb.callrecorder.AppLibrary.SQLITE_TRUE;

public class ContactsRepository {
    public interface LoadContactsCallback {
        void onContactsLoaded(List<Contact> contacts);
    }

    public static Contact getFirstContact() {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CallRecorderApplication.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.
                query(ContactsContract.Listened.TABLE_NAME, null, null, null, null, null, ContactsContract.Listened._ID + " ASC", "1");

        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            return populateContact(cursor);
        }
        return null;
    }

    public static Contact getNextContact(Contact current) {
        long id = current.getId() + 1;
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CallRecorderApplication.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.
                query(ContactsContract.Listened.TABLE_NAME, null, ContactsContract.Listened._ID + "=" + id,
                        null, null, null, null);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            return populateContact(cursor);
        }
        return null;
    }

    public static Contact getPreviousContact(Contact current) {
        long id = current.getId() - 1;
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CallRecorderApplication.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.
                query(ContactsContract.Listened.TABLE_NAME, null, ContactsContract.Listened._ID + "=" + id,
                        null, null, null, null);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            return populateContact(cursor);
        }
        return null;
    }

    private static Contact populateContact(Cursor cursor) {
        Contact contact = new Contact();
        contact.setPhoneNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_NUMBER)));
        contact.setPrivateNumber(cursor.getInt(
                cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_PRIVATE_NUMBER)) == SQLITE_TRUE);
        contact.setContactName(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_CONTACT_NAME)));
        contact.setPhotoUri(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_PHOTO_URI)));
        contact.setPhoneType(
                cursor.getInt(cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_PHONE_TYPE)));
        contact.setId(cursor.getLong(cursor.getColumnIndex(ContactsContract.Listened._ID)));
        contact.setShouldRecord(
                cursor.getInt(cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_SHOULD_RECORD)) == 1);
        contact.setUnkownNumber(cursor.getInt(cursor.getColumnIndex(ContactsContract.Listened.COLUMN_NAME_UNKNOWN_NUMBER)) == 1);
        return contact;
    }

    public static void getContacts(LoadContactsCallback callback) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CallRecorderApplication.getInstance());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<Contact> contacts = new ArrayList<>();

        Cursor cursor = db.
                query(ContactsContract.Listened.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext()) {
            contacts.add(populateContact(cursor));
        }
        cursor.close();
        Collections.sort(contacts);
        callback.onContactsLoaded(contacts);
    }
}
