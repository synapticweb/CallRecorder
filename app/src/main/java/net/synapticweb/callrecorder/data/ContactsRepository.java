/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.synapticweb.callrecorder.CrApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.synapticweb.callrecorder.CrApp.SQLITE_TRUE;

public class ContactsRepository {
    public interface LoadContactsCallback {
        void onContactsLoaded(List<Contact> contacts);
    }

    public static Contact getFirstContact() {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.
                query(ContactsContract.Contacts.TABLE_NAME, null, null, null, null, null, ContactsContract.Contacts._ID + " ASC", "1");

        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            return populateContact(cursor);
        }
        return null;
    }

    public static Contact getNextContact(Contact current) {
        long id = current.getId() + 1;
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.
                query(ContactsContract.Contacts.TABLE_NAME, null, ContactsContract.Contacts._ID + "=" + id,
                        null, null, null, null);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            return populateContact(cursor);
        }
        return null;
    }

    public static Contact getPreviousContact(Contact current) {
        long id = current.getId() - 1;
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor = db.
                query(ContactsContract.Contacts.TABLE_NAME, null, ContactsContract.Contacts._ID + "=" + id,
                        null, null, null, null);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            return populateContact(cursor);
        }
        return null;
    }

    private static Contact populateContact(Cursor cursor) {
        Contact contact = new Contact();
        contact.setPhoneNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_NUMBER)));
        contact.setPrivateNumber(cursor.getInt(
                cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_PRIVATE_NUMBER)) == SQLITE_TRUE);
        contact.setContactName(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME)));
        contact.setPhotoUri(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_PHOTO_URI)));
        contact.setPhoneType(
                cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_PHONE_TYPE)));
        contact.setId(cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID)));

        return contact;
    }

    public static void getContacts(LoadContactsCallback callback) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<Contact> contacts = new ArrayList<>();

        Cursor cursor = db.
                query(ContactsContract.Contacts.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext()) {
            contacts.add(populateContact(cursor));
        }
        cursor.close();
        Collections.sort(contacts);
        callback.onContactsLoaded(contacts);
    }
}
