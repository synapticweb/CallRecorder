package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsRepository;
import net.synapticweb.callrecorder.data.ContactsContract;
import net.synapticweb.callrecorder.data.CallRecorderDbHelper;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

        ImageButton detailMenu = parentActivity.findViewById(R.id.contact_detail_menu);
        ImageButton editContact = parentActivity.findViewById(R.id.edit_contact);
        ImageButton callContact = parentActivity.findViewById(R.id.call_contact);
        if(contact != null) {
            ContactDetailFragment contactDetail = ContactDetailFragment.newInstance(contact);
            parentActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contact_detail_fragment_container, contactDetail)
                    .commitAllowingStateLoss(); //fără chestia asta îmi dă un Caused by:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState cînd înlocuiesc fragmentul detail după adăugarea unui
            //contact nou. Soluția: https://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
        }
        else {
            //celelalte butoane nu pot să fie vizibile deoarece ștergerea unui contact nu se poate face cu selectMode on
            detailMenu.setVisibility(View.GONE);
            editContact.setVisibility(View.GONE);
            callContact.setVisibility(View.GONE);

            Fragment detailFragment = parentActivity.getSupportFragmentManager().findFragmentById(R.id.contact_detail_fragment_container);
            if(detailFragment != null) //dacă aplicația începe fără niciun contact detailFragment va fi null
                parentActivity.getSupportFragmentManager().beginTransaction().remove(detailFragment).commit();
        }
    }

    @Override
    public void manageContactDetails(Contact contact, int previousSelectedPosition, int currentSelectedPosition) {
        if(view.isSinglePaneLayout())
            view.startContactDetailActivity(contact);
        else {
            setCurrentDetail(contact);
            View currentSelectedView = view.getContactsRecycler().getLayoutManager().findViewByPosition(currentSelectedPosition);
            View previousSelectedView = view.getContactsRecycler().getLayoutManager().findViewByPosition(previousSelectedPosition);
            view.selectContact(currentSelectedView);
            view.getContactsAdapter().notifyItemChanged(currentSelectedPosition);
            view.deselectContact(previousSelectedView);
            view.getContactsAdapter().notifyItemChanged(previousSelectedPosition);
        }
    }

    @Override
    public void addNewContact() {
        Fragment fragment = (Fragment) view;
        Intent pickNumber = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        fragment.startActivityForResult(pickNumber, REQUEST_ADD_CONTACT);
    }

    @Override
    public void onAddContactResult(Intent intent) {
        Uri numberUri;
        String newNumber = null, contactName = null, photoUri = null;
        int phoneType = CrApp.UNKNOWN_TYPE_PHONE_CODE;
        Phonenumber.PhoneNumber phoneNumberWrapper;

        if(intent != null && (numberUri = intent.getData()) != null) {
            Cursor cursor = view.getParentActivity().getContentResolver().
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
                fireAlert(view.getParentActivity(),R.string.information_title, R.string.number_invalid_message,
                        android.R.string.ok, null, null);
                return ;
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

            if (match) {
               fireAlert(view.getParentActivity(), R.string.information_title, R.string.number_exists_message,
                       android.R.string.ok, null, null);
                return ;
            }

            Contact contact = new Contact(null, newNumber, contactName, photoUri, phoneType);
            try {
                contact.insertInDatabase(CrApp.getInstance());
            }
            catch (SQLException exc) {
                Log.wtf(TAG, exc.getMessage());
            }
            view.setNewAddedContactId(contact.getId());
        }
    }

    private void fireAlert(Activity activity, int title, int message, int positiveText, Integer negativeText,
                           MaterialDialog.SingleButtonCallback onPositiveCallback) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                .title(title)
                .content(message)
                .positiveText(positiveText)
                .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.warning));

                if(negativeText != null)
                    builder.negativeText(negativeText);
                if(onPositiveCallback != null)
                    builder.onPositive(onPositiveCallback);

                builder.show();
    }
}
