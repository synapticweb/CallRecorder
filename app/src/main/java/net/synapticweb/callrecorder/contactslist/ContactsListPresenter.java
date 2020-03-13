/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrApp.DialogInfo;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsRepository;
import net.synapticweb.callrecorder.data.ContactsContract;
import net.synapticweb.callrecorder.data.CallRecorderDbHelper;
import java.util.List;
import androidx.annotation.NonNull;


public class ContactsListPresenter implements ContactsListContract.ContactsListPresenter {
    @NonNull private ContactsListContract.View view;

    ContactsListPresenter(@NonNull ContactsListContract.View view) {
        this.view = view;
    }

    @Override
    public void loadContacts() {
        ContactsRepository.getContacts((List<Contact> contacts) -> view.showContacts(contacts));
    }

    @Override
    public DialogInfo addContactResult(Intent intent) {
        Uri numberUri;
        String newNumber = null, contactName = null, photoUri = null;
        int phoneType = CrApp.UNKNOWN_TYPE_PHONE_CODE;
        Phonenumber.PhoneNumber phoneNumberWrapper;

        if(intent != null && (numberUri = intent.getData()) != null) {
            Cursor cursor = CrApp.getInstance().getContentResolver().
                    query(numberUri, new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                                    android.provider.ContactsContract.CommonDataKinds.Phone.TYPE},
                            null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                newNumber = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)) ;
                contactName = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                photoUri = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                phoneType = cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE));
                cursor.close();
            }

            final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String countryCode = CrApp.getUserCountry(CrApp.getInstance());
            if(countryCode == null)
                countryCode = "US";
            try {
               phoneNumberWrapper = phoneUtil.parse(newNumber, countryCode);
            }
            catch (NumberParseException exc) {
                return new DialogInfo(R.string.information_title, R.string.number_invalid_message, R.drawable.warning);
            }

            CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            cursor = db.query(
                    ContactsContract.Contacts.TABLE_NAME, new String[]{ContactsContract.Contacts.COLUMN_NAME_NUMBER},
                    null, null, null, null, null);

            boolean match = false;
            while (cursor.moveToNext()) {
                PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(phoneNumberWrapper, cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_NUMBER)));
                if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                    match = true;
                    break;
                }
            }
            cursor.close();

            if (match)
                return new DialogInfo(R.string.information_title, R.string.number_exists_message, R.drawable.warning);

            Contact contact = new Contact(null, newNumber, contactName, photoUri, phoneType);
            try {
                contact.save();
            }
            catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, "Error inserting contact in database: " + exc.getMessage());
                return new DialogInfo(R.string.error_title, R.string.error_insert_contact, R.drawable.error);
            }
        }
        return null;
    }
}
