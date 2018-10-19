package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.CallRecorderApplication;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsRepository;
import net.synapticweb.callrecorder.data.ListenedContract;
import net.synapticweb.callrecorder.data.RecordingsDbHelper;

import java.io.IOException;
import java.util.List;

import static net.synapticweb.callrecorder.AppLibrary.UNKNOWN_TYPE_PHONE_CODE;

public class ContactsListPresenter implements ContactsListContract.ContactsListPresenter {
    @NonNull private ContactsListContract.View view;
    static final int REQUEST_ADD_CONTACT = 1;
    private static final String TAG = "CallRecorder";

    ContactsListPresenter(@NonNull ContactsListContract.View view) {
        this.view = view;
    }

    @Override
    public void loadContacts() {
        ContactsRepository.getContacts(new ContactsRepository.LoadContactsCallback() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                view.showContacts(contacts);
            }
        });
    }

    @Override
    public void setCurrentDetail(Contact contact) {
        Fragment fragment = (Fragment) view;
        AppCompatActivity parentActivity = (AppCompatActivity) fragment.getActivity();
        if(parentActivity == null)
            return ;

        ImageButton detailMenu = parentActivity.findViewById(R.id.phone_number_detail_menu);
        if(contact != null) {
            detailMenu.setVisibility(View.VISIBLE);

            ContactDetailFragment contactDetail = ContactDetailFragment.newInstance(contact);
            parentActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contact_detail_fragment_container, contactDetail)
                    .commitAllowingStateLoss(); //fără chestia asta îmi dă un Caused by:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState cînd înlocuiesc fragmentul detail după adăugarea unui
            //contact nou. Soluția: https://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
        }
        else {
            detailMenu.setVisibility(View.GONE);

            Fragment detailFragment = parentActivity.getSupportFragmentManager().findFragmentById(R.id.contact_detail_fragment_container);
            if(detailFragment != null) //dacă aplicația începe fără niciun contact detailFragment va fi null
                parentActivity.getSupportFragmentManager().beginTransaction().remove(detailFragment).commit();
        }
    }

    @Override
    public void manageContactDetails(Contact contact, View previousSelected, View currentSelected) {
        if(view.isSinglePaneLayout())
            view.startContactDetailActivity(contact);
        else {
            setCurrentDetail(contact);
            view.markSelectedContact(previousSelected, currentSelected);
        }
    }

    private void alertAtInsertContact(int message, Activity activity) {
        new MaterialDialog.Builder(activity)
                .title(R.string.number_exists_title)
                .content(CallRecorderApplication.getInstance().getResources().getString(message))
                .positiveText(android.R.string.ok)
                .icon(CallRecorderApplication.getInstance().getResources().getDrawable(R.drawable.warning))
                .show();
    }

    @Override
    public void addNewContact() {
        Fragment fragment = (Fragment) view;
        Intent pickNumber = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        fragment.startActivityForResult(pickNumber, REQUEST_ADD_CONTACT);
    }

    @Override
    public void onAddContactResult(Intent intent) {
        Uri numberUri;
        String newNumber = null, contactName = null, photoUri = null;
        int phoneType = UNKNOWN_TYPE_PHONE_CODE;

        if(intent != null && (numberUri = intent.getData()) != null) {
            Cursor cursor = view.getParentActivity().getContentResolver().
                    query(numberUri, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                                    ContactsContract.CommonDataKinds.Phone.TYPE},
                            null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                newNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                cursor.close();
            }

            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String countryCode = AppLibrary.getUserCountry(CallRecorderApplication.getInstance());
            if(countryCode == null)
                countryCode = "US";

            if(!phoneUtil.isPossibleNumber(newNumber, countryCode)) {
                alertAtInsertContact(R.string.number_impossible, view.getParentActivity());
                return ;
            }

            RecordingsDbHelper mDbHelper = new RecordingsDbHelper(CallRecorderApplication.getInstance());
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            cursor = db.query(
                    ListenedContract.Listened.TABLE_NAME, new String[]{ListenedContract.Listened.COLUMN_NAME_NUMBER},
                    null, null, null, null, null);

            boolean match = false;
            while (cursor.moveToNext()) {
                PhoneNumberUtil.MatchType matchType = phoneUtil.isNumberMatch(cursor.getString(
                        cursor.getColumnIndex(ListenedContract.Listened.COLUMN_NAME_NUMBER)), newNumber);
                if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                    match = true;
                    break;
                }
            }

            if (match)
                alertAtInsertContact(R.string.number_exists_message, view.getParentActivity());
            else
            {
                Contact contact = new Contact(null, newNumber, contactName, photoUri, phoneType);
                try {
                    contact.copyPhotoIfExternal(view.getParentActivity());
                    contact.insertInDatabase(CallRecorderApplication.getInstance());
                }
                catch (SQLException | IOException exc) {
                    Log.wtf(TAG, exc.getMessage());
                }
                view.setNewAddedContactId(contact.getId());
            }
        }
    }
}
