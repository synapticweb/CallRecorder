package net.synapticweb.callrecorder.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FakePhoneLookupProvider extends ContentProvider {
    private List<ContactData> data = new ArrayList<>();
    static final String AUTHORITY = "com.android.contacts";

    void addContact(ContactData contact) {
        data.add(contact);
    }

    private ContactData findByNumber(String number) {
        for(ContactData contact : data)
            if(contact.number.equals(number))
                return contact;
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        String number = uri.getLastPathSegment();
        String[] columns = new String[] { android.provider.ContactsContract.PhoneLookup.NUMBER,
                android.provider.ContactsContract.PhoneLookup.TYPE,
                android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME,
                android.provider.ContactsContract.PhoneLookup.PHOTO_URI };
        MatrixCursor cursor = new MatrixCursor(columns);

        ContactData contact;
        if((contact = findByNumber(number)) != null ) {
            cursor.addRow(new Object[] {contact.number, contact.type, contact.name, contact.photoUri});
        }

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    static class ContactData {
        ContactData(String name, String number, int type, String photoUri) {
            this.name = name;
            this.number = number;
            this.type = type;
            this.photoUri = photoUri;
        }

        String name;
        String number;
        int type;
        String photoUri;
    }
}
