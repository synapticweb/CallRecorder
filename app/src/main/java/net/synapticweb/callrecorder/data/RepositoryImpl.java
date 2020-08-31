package net.synapticweb.callrecorder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

public class RepositoryImpl implements Repository {
    private SQLiteDatabase database;

    @Inject
    public RepositoryImpl(Context context, String dbname) {
        SQLiteOpenHelper helper = new CallRecorderDbHelper(context, dbname);
        this.database = helper.getWritableDatabase();
    }

    private Contact populateContact(Cursor cursor) {
        Contact contact = new Contact();
        contact.setPhoneNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_NUMBER)));
        contact.setContactName(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME)));
        contact.setPhotoUri(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_PHOTO_URI)));
        contact.setPhoneType(
                cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_PHONE_TYPE)));
        contact.setId(cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
        return contact;
    }

    @Override
    public List<Contact> getAllContacts() {
        List<Contact> contacts = new ArrayList<>();
        Cursor cursor = database.
                query(ContactsContract.Contacts.TABLE_NAME, null, null, null, null, null, null);

        while(cursor.moveToNext()) {
            contacts.add(populateContact(cursor));
        }
        cursor.close();
        return contacts;
    }

    @Override
    public void getAllContacts(LoadContactsCallback callback) {
        List<Contact> contacts = getAllContacts();
        Collections.sort(contacts);
        callback.onContactsLoaded(contacts);
    }

    @VisibleForTesting
    Contact getContact(Long id) {
        Cursor cursor = database.query(ContactsContract.Contacts.TABLE_NAME, null, ContactsContract.Contacts._ID +
                "=" + id, null, null, null, null);

        Contact contact = null;
        if(cursor != null && cursor.moveToFirst()) {
           contact = populateContact(cursor);
        }
        return contact;
    }

    @Override
    public Long getHiddenNumberContactId() {
        Cursor cursor = database.query(ContactsContract.Contacts.TABLE_NAME, new String[]{ContactsContract.Contacts._ID},
                ContactsContract.Contacts.COLUMN_NAME_NUMBER + " is " + "NULL", null, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            cursor.close();
            return id;
        }
        else
            return null;
    }


    private ContentValues createContactContentValues(Contact contact) {
        ContentValues values = new ContentValues();

        values.put(ContactsContract.Contacts.COLUMN_NAME_NUMBER, contact.getPhoneNumber());
        values.put(ContactsContract.Contacts.COLUMN_NAME_CONTACT_NAME, contact.getContactName());
        values.put(ContactsContract.Contacts.COLUMN_NAME_PHOTO_URI, contact.getPhotoUri() == null ?
                null : contact.getPhotoUri().toString());
        values.put(ContactsContract.Contacts.COLUMN_NAME_PHONE_TYPE, contact.getPhoneTypeCode());
        return values;
    }

    @Override
    public void insertContact(Contact contact) throws SQLException {
        ContentValues values = createContactContentValues(contact);
        long rowId = database.insertOrThrow(ContactsContract.Contacts.TABLE_NAME, null, values);
        contact.setId(rowId);
    }

    @Override
    public void updateContact(Contact contact) throws SQLException, IllegalStateException {
        if(contact.getId() == 0)
            throw new IllegalStateException("This contact was not saved in database");

        ContentValues values = createContactContentValues(contact);
        int updatedRows = database.update(ContactsContract.Contacts.TABLE_NAME, values,
                ContactsContract.Contacts._ID + "=" + contact.getId(), null);
        if(updatedRows != 1)
            throw new SQLException("The return value of updating this contact was " + updatedRows);
    }

    @Override
    public void deleteContact(Contact contact) {
        if(contact.getId() == 0)
            throw new IllegalStateException("This contact was not saved in database");
        int deletedRows = database.delete(ContactsContract.Contacts.TABLE_NAME, ContactsContract.Contacts._ID
                + "=" + contact.getId(), null);
        if(deletedRows != 1)
            throw new SQLException("The return value of deleting this contact was " + deletedRows);
    }

    private Recording populateRecording(Cursor cursor) {
        Recording recording = new Recording();
        recording.setId(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings._ID)));
        long contactId = cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_CONTACT_ID));
        recording.setContactId(contactId == 0 ? null : contactId);
        recording.setIncoming(cursor.getInt(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_INCOMING)) == 1);
        recording.setPath(cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_PATH)));
        recording.setStartTimestamp(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_START_TIMESTAMP)));
        recording.setEndTimestamp(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP)));
        recording.setFormat(cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_FORMAT)));
        recording.setIsNameSet(cursor.getInt(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_IS_NAME_SET)) == 1);
        recording.setMode(cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_MODE)));
        recording.setSource(cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_SOURCE)));
        return recording;
    }

    @Override
    public List<Recording> getRecordings(Contact contact) {
        List<Recording> list =  new ArrayList<>();
        Cursor cursor = database.query(RecordingsContract.Recordings.TABLE_NAME,
                null, RecordingsContract.Recordings.COLUMN_NAME_CONTACT_ID + " is " + (contact == null ? "null" : contact.getId()), null, null, null, RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP + " DESC");

        while(cursor.moveToNext()) {
            list.add(populateRecording(cursor));
        }
        cursor.close();
        return list;
    }

    @Override
    public void getRecordings(Contact contact, LoadRecordingsCallback callback) {
        callback.onRecordingsLoaded(getRecordings(contact));
    }

    @VisibleForTesting
    Recording getRecording(Long id) {
        Cursor cursor = database.query(RecordingsContract.Recordings.TABLE_NAME, null, RecordingsContract.Recordings._ID +
                "=" + id, null, null, null, null);

        Recording recording = null;
        if(cursor != null && cursor.moveToFirst()) {
            recording = populateRecording(cursor);
        }
        return recording;
    }

    private ContentValues createRecordingContentValues(Recording recording) {
        ContentValues values = new ContentValues();

        values.put(RecordingsContract.Recordings.COLUMN_NAME_CONTACT_ID, recording.getContactId());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_PATH, recording.getPath());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_INCOMING, recording.isIncoming());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_START_TIMESTAMP, recording.getStartTimestamp());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP, recording.getEndTimestamp());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_IS_NAME_SET, recording.getIsNameSet());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_FORMAT, recording.getFormat());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_MODE, recording.getMode());
        values.put(RecordingsContract.Recordings.COLUMN_NAME_SOURCE, recording.getSource());
        return values;
    }

    @Override
    public void insertRecording(Recording recording) {
        ContentValues values = createRecordingContentValues(recording);
        long rowId = database.insertOrThrow(RecordingsContract.Recordings.TABLE_NAME, null, values);
        recording.setId(rowId);
    }

    @Override
    public void updateRecording(Recording recording) throws IllegalStateException, SQLException {
        if(recording.getId() == 0)
            throw new IllegalStateException("This contact was not saved in database");

        ContentValues values = createRecordingContentValues(recording);
        int updatedRows = database.update(RecordingsContract.Recordings.TABLE_NAME, values,
                RecordingsContract.Recordings._ID + "=" + recording.getId(), null);
        if(updatedRows != 1)
            throw new SQLException("The return value of updating this recording was " + updatedRows);
    }

    @Override
    public void deleteRecording(Recording recording) throws IllegalStateException, SQLException {
        if(recording.getId() == 0)
            throw new IllegalStateException("This recording was not saved in database");
        int deletedRows = database.delete(RecordingsContract.Recordings.TABLE_NAME,
                RecordingsContract.Recordings._ID + "=" + recording.getId(), null);
        if(deletedRows != 1)
            throw new SQLException("The return value of deleting this recording was " + deletedRows);
    }

    @VisibleForTesting
    void closeDb() {
        database.close();
    }
}
