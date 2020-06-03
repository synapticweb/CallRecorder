/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import net.synapticweb.callrecorder.Util.DialogInfo;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Repository;
import net.synapticweb.callrecorder.di.FragmentScope;

import androidx.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

@FragmentScope
public class ContactsListPresenter implements ContactsListContract.Presenter {
    @NonNull private ContactsListContract.View view;
    private Repository repository;

    @Inject
    ContactsListPresenter(@NonNull ContactsListContract.View view, Repository repository) {
        this.view = view;
        this.repository = repository;
    }

    @Override
    public void loadContacts() {
        repository.getAllContacts(contacts -> view.showContacts(contacts));
    }

    @Override
    public DialogInfo addContactResult(Intent intent, Context context) {
        Uri numberUri;
        String newNumber = null, contactName = null, photoUri = null;
        int phoneType = Util.UNKNOWN_TYPE_PHONE_CODE;
        Phonenumber.PhoneNumber phoneNumberWrapper;

        if(intent != null && (numberUri = intent.getData()) != null) {
            Cursor cursor = context.getContentResolver().
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
            String countryCode = Util.getUserCountry(context);
            if(countryCode == null)
                countryCode = "US";
            try {
               phoneNumberWrapper = phoneUtil.parse(newNumber, countryCode);
            }
            catch (NumberParseException exc) {
                return new DialogInfo(R.string.information_title, R.string.number_invalid_message, R.drawable.warning);
            }

            List<Contact> contacts = repository.getAllContacts();

            boolean match = false;
            for(Contact contact : contacts) {
                PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(phoneNumberWrapper, contact.getPhoneNumber());
                if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                    match = true;
                    break;
                }
            }

            if (match)
                return new DialogInfo(R.string.information_title, R.string.number_exists_message, R.drawable.warning);

            Contact contact = new Contact(null, newNumber, contactName, photoUri, phoneType);
            try {
                contact.save(repository);
            }
            catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, "Error inserting contact in database: " + exc.getMessage());
                return new DialogInfo(R.string.error_title, R.string.error_insert_contact, R.drawable.error);
            }
        }
        return null;
    }
}
