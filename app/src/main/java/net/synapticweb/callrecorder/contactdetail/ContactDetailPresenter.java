/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;


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
import net.synapticweb.callrecorder.data.ContactsContract;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.data.CallRecorderDbHelper;
import net.synapticweb.callrecorder.data.RecordingsRepository;

import java.io.File;
import java.util.List;


public class ContactDetailPresenter implements ContactDetailContract.ContactDetailPresenter {
    private ContactDetailContract.View view;

     ContactDetailPresenter(ContactDetailContract.View view) {
        this.view = view;
    }

    @Override
    public DialogInfo deleteContact(Contact contact) {
         try {
             contact.delete();
         }
         catch (Exception exc) {
             CrLog.log(CrLog.ERROR, "Error deleting the contact: " + exc.getMessage());
             return new DialogInfo(R.string.error_title, R.string.error_deleting_contact, R.drawable.error);
         }
         return null;
    }

    @Override
    public DialogInfo renameRecording(CharSequence input, Recording recording) {
        if(Recording.hasIllegalChar(input))
            return new DialogInfo(R.string.information_title, R.string.rename_illegal_chars, R.drawable.info);

        String parent = new File(recording.getPath()).getParent();
        String oldFileName = new File(recording.getPath()).getName();
        String ext = oldFileName.substring(oldFileName.length() - 3);
        String newFileName = input + "." + ext;

        if(new File(parent, newFileName).exists())
            return new DialogInfo(R.string.information_title, R.string.rename_already_used, R.drawable.info);

        try {
            if(new File(recording.getPath()).renameTo(new File(parent, newFileName)) ) {
                recording.setPath(new File(parent, newFileName).getAbsolutePath());
                recording.setIsNameSet(true);
                recording.updateRecording(CrApp.getInstance());
            }
            else
                throw new Exception("File.renameTo() has returned false.");
        }
        catch (Exception e) {
            CrLog.log(CrLog.ERROR, "Error renaming the recording:" + e.getMessage());
            return new DialogInfo(R.string.error_title, R.string.rename_error, R.drawable.error);
        }

        return null;
    }

    @Override
    public void loadRecordings(Contact contact) {
        RecordingsRepository.getRecordings(contact, (List<Recording> recordings) -> {
                view.paintViews(recordings);
                //aici ar trebui să fie cod care să pună tickuri pe recordingurile selectate cînd
                //este întors device-ul. Dar dacă pun aici codul nu se execută pentru că nu vor fi gata
                //cardview-urile. Așa că acest cod se duce în RecordingAdapter::onBindViewHolder()
                // total - neintuitiv.
            });
    }

    @Override
    public DialogInfo deleteRecordings(List<Recording> recordings) {
        for(Recording recording :  recordings) {
            try {
                recording.delete();
                view.removeRecording(recording);
            }
            catch (Exception exc) {
                CrLog.log(CrLog.ERROR, "Error deleting the selected recording(s): " + exc.getMessage());
                return new DialogInfo(R.string.error_title, R.string.error_deleting_recordings, R.drawable.error);
            }
        }
        return null;
    }


    @Override
    public DialogInfo assignToContact(Uri numberUri, List<Recording> recordings, Contact contact) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumberWrapper;
        Cursor cursor;
        String phoneNumber = null, contactName = null, photoUri = null;
        int phoneType = CrApp.UNKNOWN_TYPE_PHONE_CODE;
        PhoneNumberUtil.MatchType matchType;
        long contactId = 0;

        cursor = CrApp.getInstance().getContentResolver().
                query(numberUri, new String[]{android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                                android.provider.ContactsContract.CommonDataKinds.Phone.TYPE},
                        null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
            contactName = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            photoUri = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
            phoneType = cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE));
            cursor.close();
        }

        String countryCode = CrApp.getUserCountry(CrApp.getInstance());
        if(countryCode == null)
            countryCode = "US";
        try {
            phoneNumberWrapper = phoneUtil.parse(phoneNumber, countryCode);
        }
        catch (NumberParseException exc) {
            return new DialogInfo(R.string.information_title,
                    R.string.number_invalid_message, R.drawable.warning);
        }

        if(contact != null) {
            matchType = phoneUtil.isNumberMatch(phoneNumberWrapper, contact.getPhoneNumber());
            if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                return new DialogInfo(R.string.information_title,
                        R.string.assign_to_same_contact, R.drawable.warning);
            }
        }
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        cursor = db.query(ContactsContract.Contacts.TABLE_NAME,
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.COLUMN_NAME_NUMBER},
                null, null, null, null, null);

        while(cursor.moveToNext()) {
            String number = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.COLUMN_NAME_NUMBER));
            long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            matchType = phoneUtil.isNumberMatch(phoneNumberWrapper, number);
            if(matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                contactId = id;
                break;
            }
        }
        cursor.close();
        if(contactId == 0) {
            Contact newContact = new Contact(null, phoneNumber, contactName, photoUri, phoneType);
            try {
                newContact.insertInDatabase(CrApp.getInstance());
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, exc.getMessage());
                return new DialogInfo(R.string.error_title,
                        R.string.assign_to_contact_err, R.drawable.error);
            }
            contactId = newContact.getId();
        }

        for(Recording recording : recordings) {
            recording.setContactId(contactId);
            try {
                recording.updateRecording(CrApp.getInstance());
                view.removeRecording(recording);
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, exc.getMessage());
                return new DialogInfo(R.string.error_title,
                        R.string.assign_to_contact_err, R.drawable.error);
            }
        }

        return new DialogInfo(R.string.information_title,
                R.string.assign_to_contact_ok, R.drawable.success);
    }

    @Override
    public DialogInfo assignToPrivate(List<Recording> recordings,  Contact contact) {
         if(contact != null && contact.isPrivateNumber()) {
             return new DialogInfo(R.string.information_title, R.string.assign_to_same_contact, R.drawable.warning);
         }

        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id;

        Cursor cursor = db.query(ContactsContract.Contacts.TABLE_NAME, new String[]{ContactsContract.Contacts._ID},
                ContactsContract.Contacts.COLUMN_NAME_PRIVATE_NUMBER + "=" + CrApp.SQLITE_TRUE, null, null, null, null);

        if (cursor.moveToFirst())
            id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        else {
            Contact newContact = new Contact();
            newContact.setPrivateNumber(true);
            try {
                newContact.insertInDatabase(CrApp.getInstance());
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
                return new DialogInfo(R.string.error_title, R.string.assign_to_contact_err, R.drawable.error);
            }
            id = newContact.getId();
        }
        cursor.close();

        for (Recording recording : recordings) {
            recording.setContactId(id);
            try {
                recording.updateRecording(CrApp.getInstance());
                view.removeRecording(recording);
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, exc.getMessage());
                return new DialogInfo(R.string.error_title, R.string.assign_to_contact_err, R.drawable.error);
            }
        }

        return new DialogInfo(R.string.information_title, R.string.assign_to_contact_ok, R.drawable.success);
        }
}
