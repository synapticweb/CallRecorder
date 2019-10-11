/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;


import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsContract;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.data.CallRecorderDbHelper;
import net.synapticweb.callrecorder.data.RecordingsRepository;
import net.synapticweb.callrecorder.player.PlayerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class ContactDetailPresenter implements ContactDetailContract.ContactDetailPresenter {
    private ContactDetailContract.View view;
    static final int EDIT_REQUEST_CODE = 1;
    static final String EDIT_EXTRA_CONTACT = "edit_extra_contact";
    public static final String RECORDING_EXTRA = "recording_extra";

     ContactDetailPresenter(ContactDetailContract.View view) {
        this.view = view;
    }

    @Override
    public void storageInfo() {
        long sizePrivate = 0, sizePublic = 0;
        for(Recording recording : view.getRecordingsAdapter().getRecordings()) {
            long size = new File(recording.getPath()).length();
            if(recording.isSavedInPrivateSpace())
                sizePrivate += size;
            else
                sizePublic += size;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(view.getParentActivity())
                .title(R.string.storage_info)
                .customView(R.layout.info_storage_dialog, false)
                .positiveText(android.R.string.ok).build();
        TextView privateStorage = dialog.getView().findViewById(R.id.info_storage_private_data);
        privateStorage.setText(CrApp.getFileSizeHuman(sizePrivate));

        TextView publicStorage = dialog.getView().findViewById(R.id.info_storage_public_data);
        publicStorage.setText(CrApp.getFileSizeHuman(sizePublic));

        dialog.show();
    }

    @Override
    public void onRenameClick() {
        new MaterialDialog.Builder(view.getParentActivity())
                .title(R.string.rename_recording_title)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(CrApp.getInstance().getResources().getString(R.string.rename_recording_input_text),
                        null, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        if(view.getSelectedItems().size() != 1) {
                            CrLog.log(CrLog.WARN, "Calling onRenameClick when multiple recordings are selected");
                            return ;
                        }
                        if(Recording.hasIllegalChar(input)) {
                            new MaterialDialog.Builder(view.getParentActivity())
                                    .title(R.string.information_title)
                                    .content(R.string.rename_illegal_chars)
                                    .positiveText("OK")
                                    .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.info))
                                    .show();
                            return;
                        }

                        Recording selRec = view.getSelectedRecordings().get(0); //de testat. size == 1
                        String parent = new File(selRec.getPath()).getParent();
                        String oldFileName = new File(selRec.getPath()).getName();
                        String ext = oldFileName.substring(oldFileName.length() - 3);
                        String newFileName = input + "." + ext;

                        if(new File(parent, newFileName).exists()) {
                            new MaterialDialog.Builder(view.getParentActivity())
                                    .title(R.string.information_title)
                                    .content(R.string.rename_already_used)
                                    .positiveText("OK")
                                    .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.info))
                                    .show();
                            return;
                        }
                        try {
                            if(new File(selRec.getPath()).renameTo(new File(parent, newFileName)) ) {
                                selRec.setPath(new File(parent, newFileName).getAbsolutePath());
                                selRec.setIsNameSet(true);
                                selRec.updateRecording(CrApp.getInstance());
                                view.getRecordingsAdapter().notifyItemChanged(view.getSelectedItems().get(0));
                            }
                            else
                                throw new Exception("File.renameTo() has returned false.");
                        }
                        catch (Exception e) {
                            CrLog.log(CrLog.ERROR, "Error renaming the recording:" + e.getMessage());
                            new MaterialDialog.Builder(view.getParentActivity())
                                    .title(R.string.error_title)
                                    .content(R.string.rename_error)
                                    .positiveText("OK")
                                    .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.error))
                                    .show();
                        }
                    }

                }).show();
    }

    @Override
    public void onInfoClick() {
        if(view.getSelectedItems().size() > 1) {
            long totalSize = 0;
            for(int position : view.getSelectedItems()) {
                Recording recording = view.getRecordingsAdapter().getItem(position);
                totalSize += recording.getSize();
            }

            new MaterialDialog.Builder(view.getParentActivity())
                    .title(R.string.recordings_info_title)
                    .content(String.format(CrApp.getInstance().getResources().getString(R.string.recordings_info_text), CrApp.getFileSizeHuman(totalSize)))
                    .positiveText(android.R.string.ok)
                    .show();
            return ;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(view.getParentActivity())
                .title(R.string.recording_info_title)
                .customView(R.layout.info_dialog, false)
                .positiveText(android.R.string.ok).build();

        //There should be only one if we are here:
        if(view.getSelectedItems().size() != 1) {
            CrLog.log(CrLog.WARN, "Calling onInfoClick when multiple recordings are selected");
            return ;
        }
        final Recording recording = view.getRecordingsAdapter().getItem(view.getSelectedItems().get(0));
        TextView date = dialog.getView().findViewById(R.id.info_date_data);
        date.setText(String.format("%s %s", recording.getDate(false), recording.getTime()));
        TextView size = dialog.getView().findViewById(R.id.info_size_data);
        size.setText(CrApp.getFileSizeHuman(recording.getSize()));

        TextView format = dialog.getView().findViewById(R.id.info_format_data);
        format.setText(recording.getHumanReadingFormat());
        TextView length = dialog.getView().findViewById(R.id.info_length_data);
        length.setText(CrApp.getDurationHuman(recording.getLength(), true));
        TextView path = dialog.getView().findViewById(R.id.info_path_data);
        path.setText(recording.isSavedInPrivateSpace() ? CrApp.getInstance().getResources().
                getString(R.string.private_storage) : recording.getPath());
        if(!recording.exists()) {
            path.setText(String.format("%s%s", path.getText(), CrApp.getInstance().getResources().
                    getString(R.string.nonexistent_file)));
            path.setTextColor(CrApp.getInstance().getResources().getColor(android.R.color.holo_red_light));
        }

        dialog.show();
    }

    @Override
    public void deleteContact(final Contact contact) {
        final AppCompatActivity parentActivity = view.getParentActivity();
        new MaterialDialog.Builder(parentActivity)
                .title(R.string.delete_contact_confirm_title)
                .content(String.format(parentActivity.getResources().
                        getString(R.string.delete_contact_confirm_message), contact.getContactName()))
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .icon(parentActivity.getResources().getDrawable(R.drawable.warning))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            contact.delete(parentActivity);
                        }
                        catch (Exception exc) {
                            CrLog.log(CrLog.ERROR, "Error deleting the contact: " + exc.getMessage());
                        }
                        if(!view.isSinglePaneLayout()) {
                            ContactsListFragment listFragment = (ContactsListFragment)
                                    parentActivity.getSupportFragmentManager().findFragmentById(R.id.contacts_list_fragment_container);
                            listFragment.resetCurrentPosition();
                            listFragment.setContactDeleted(true);
                            listFragment.onResume();
                        }
                        else
                            view.getParentActivity().finish();
                    }
                })
                .show();
    }

    @Override
    public void editContact(final Contact contact) {
        Fragment fragment = (Fragment) view;
        Intent intent = new Intent(CrApp.getInstance(), EditContactActivity.class);
        intent.putExtra(EDIT_EXTRA_CONTACT, contact);
        fragment.startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    @Override
    public void onEditActivityResult(Bundle result) {
        if (result != null) {
            view.setContact((Contact) result.getParcelable(EditContactActivity.EDITED_CONTACT));
            if(view.isSinglePaneLayout())
                view.setActionBarTitleIfActivityDetail();
        }
    }

    @Override
    public void loadRecordings(Contact contact) {
        RecordingsRepository.getRecordings(contact, new RecordingsRepository.loadRecordingsCallback() {
            @Override
            public void onRecordingsLoaded(List<Recording> recordings) {
                view.paintViews(recordings);
                //aici ar trebui să fie cod care să pună tickuri pe recordingurile selectate cînd
                //este întors device-ul. Dar dacă pun aici codul nu se execută pentru că nu vor fi gata
                //cardview-urile. Așa că acest cod se duce în RecordingAdapter::onBindViewHolder()
                // total - neintuitiv.
            }
        });
    }

    //Ideea este ca la long click pe un recording marginile recordingului să se modifice și să apară checkboxul.
    //De asemenea la închiderea select mode marginile se modifică din nou și checkboxul dispare.
    //ContactDetailFragment.toggleSelectModeRecording() face acest lucru, dar se pune problema cum să fie apelată
    //această funcție. O posibilitate este ca ContactDetailPresenter.selectRecording() și ContactDetailFragment.
    //clearSelectMode() doar să apeleze adapter.notifyDataSetChanged() și sarcina de a apela toggleSelectModeRecording()
    //să revină RecordingAdapter.onBindViewHolder(), care este apelată pentru fiecare recording în urma adapter.notifyDataSetChanged()
    //Această soluție nu funcționa bine cu animația. Dacă foloseam animație apăreau buguri ciudate - e.g. după long
    //press recordingurile care nu fuseseră vizibile nu se modificau, etc. Dacă scoteam animația totul funcționa
    //perfect.
    //Soluția a fost ca animația să fie inclusă condițional - doar dacă toggleSelectModeRecording() este apelat
    //din ContactDetailPresenter.selectRecording() sau din clearSelectMode. Pentru că această funcție trebuie apelată
    //și cînd aplicația revine din background sau cînd ecranul este întors. Cînd toggleSelectModeRecording() este apelat
    //din una din cele 2 funcții specificate apelul specifică folosirea animației. Pentru recordingurile care încă
    //nu sunt vizibile (sunt null) nu se apelează (evident!) toggleSelectModeRecording(), dar se apelează notifyItemChanged(),
    //care îl face pe adapter să apeleze onBindViewHolder() pentru recordingul respectiv ceea ce face să se apeleze
    //toggleSelectModeRecording() fără animație. Tot fără animație se apelează această funcție la pornirea activității
    //la întoarcerea din background și la rotire. Acest setup rezolvă problema descrisă.
    @Override
    public void selectRecording(android.view.View recording, int adapterPosition, boolean exists) {
         if(!view.isSelectModeOn()) {
             view.setSelectMode(true);
             view.toggleSelectModeActionBar(true);
             for(int i = 0; i < view.getRecordingsAdapter().getItemCount(); ++i) {
                 View recordingSlot = view.getRecordingsRecycler().getLayoutManager().findViewByPosition(i);
                 if(recordingSlot != null)
                    view.toggleSelectModeRecording(recordingSlot, true);
                 view.getRecordingsAdapter().notifyItemChanged(i);
             }
         }
         if(!view.removeIfPresentInSelectedItems(adapterPosition)) {
             view.addToSelectedItems(adapterPosition);
             view.selectRecording(recording);
             if(!exists) {
                 view.setSelectedItemsDeleted(view.getSelectedItemsDeleted() + 1);
                 view.disableMoveBtn();
             }
         }
         else {
             view.deselectRecording(recording);
             if(!exists)
                 view.setSelectedItemsDeleted(view.getSelectedItemsDeleted() - 1);
             if(view.getSelectedItemsDeleted() == 0)
                 view.enableMoveBtn();
         }

         if(view.isEmptySelectedItems())
             view.clearSelectedMode();
         else
             view.updateTitle();
//         Log.wtf(TAG, view.getSelectedItems().toString());
    }

    @Override
    public void startPlayerActivity(Recording recording) {
        Intent playIntent = new Intent(CrApp.getInstance(), PlayerActivity.class);
        playIntent.putExtra(RECORDING_EXTRA, recording);
        view.getParentActivity().startActivity(playIntent);
    }

    @Override
    public void deleteSelectedRecordings() {
        for(Recording recording :  view.getSelectedRecordings()) {
            try {
                recording.delete(CrApp.getInstance());
                view.getRecordingsAdapter().removeItem(recording);
            }
            catch (Exception exc) {
                CrLog.log(CrLog.ERROR, "Error deleting the selected recording(s): " + exc.getMessage());
            }
        }
        if(view.getRecordingsAdapter().getItemCount() == 0) {
            View noContent  = view.getParentActivity().findViewById(R.id.no_content_detail);
            if(noContent != null)
                noContent.setVisibility(View.VISIBLE);
        }
        view.clearSelectedMode();
    }

    @Override
    public void moveSelectedRecordings(String path) {
         int totalSize = 0;
         List<Recording> recordings = view.getSelectedRecordings();
         Recording[] recordingsArray = new Recording[recordings.size()];

         for(Recording recording : recordings) {
             if(new File(recording.getPath()).getParent().equals(path)) {
                 new MaterialDialog.Builder(view.getParentActivity())
                         .title(R.string.information_title)
                         .content(R.string.move_destination_same)
                         .positiveText("OK")
                         .icon(view.getParentActivity().getResources().getDrawable(R.drawable.info))
                         .show();
                 return ;
             }
             totalSize += new File(recording.getPath()).length();
         }

        new MoveAsyncTask(path, totalSize, view.getParentActivity()).
                execute(recordings.toArray(recordingsArray));
    }

    @Override
    public void callContact(Contact contact) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.getPhoneNumber(), null));
        view.getParentActivity().startActivity(intent);
    }

    @Override
    public void toggleSelectAll() {
        ContactDetailFragment.RecordingAdapter adapter = view.getRecordingsAdapter();
        List<Integer> selectedItems = view.getSelectedItems();

        List<Integer> notSelected = new ArrayList<>();
        for(int i = 0; i < adapter.getItemCount(); ++i)
            notSelected.add(i);
        notSelected.removeAll(selectedItems);

        RecyclerView recordingsRecycler = view.getRecordingsRecycler();
        for(int position : notSelected) {
            view.addToSelectedItems(position);
            adapter.notifyItemChanged(position);
            //https://stackoverflow.com/questions/33784369/recyclerview-get-view-at-particular-position
            View selectedRecording = recordingsRecycler.getLayoutManager().findViewByPosition(position);
            if(selectedRecording != null) //dacă recordingul nu este încă afișat pe ecran
                // (sunt multe recordinguri și se scrolează) atunci selectedRecording va fi null. Dar mai înainte am
                //notificat adapterul că s-a schimbat, ca să îl reconstruiască.
                view.selectRecording(selectedRecording);
        }
        view.updateTitle();
    }

    void assignToContact(Uri numberUri, List<Recording> recordings, Contact contact) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumberWrapper;
        Cursor cursor;
        String phoneNumber = null, contactName = null, photoUri = null;
        int phoneType = CrApp.UNKNOWN_TYPE_PHONE_CODE;
        PhoneNumberUtil.MatchType matchType;
        long contactId = 0;

        cursor = view.getParentActivity().getContentResolver().
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
            MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                    .title(R.string.information_title)
                    .content(R.string.number_invalid_message)
                    .positiveText(android.R.string.ok)
                    .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.warning));
            builder.show();
            return ;
        }

        if(contact != null) {
            matchType = phoneUtil.isNumberMatch(phoneNumberWrapper, contact.getPhoneNumber());
            if (matchType != PhoneNumberUtil.MatchType.NO_MATCH && matchType != PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                        .title(R.string.information_title)
                        .content(R.string.assign_to_same_contact)
                        .positiveText(android.R.string.ok)
                        .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.warning));
                builder.show();
                return;
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
                MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                        .title(R.string.error_title)
                        .content(R.string.assign_to_contact_err)
                        .positiveText(android.R.string.ok)
                        .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.error));
                builder.show();
                return;
            }
            contactId = newContact.getId();
        }

        for(Recording recording : recordings) {
            recording.setContactId(contactId);
            try {
                recording.updateRecording(CrApp.getInstance());
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, exc.getMessage());
                MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                        .title(R.string.error_title)
                        .content(R.string.assign_to_contact_err)
                        .positiveText(android.R.string.ok)
                        .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.error));
                builder.show();
                return;
            }
            view.getRecordingsAdapter().removeItem(recording);
            if(view.getRecordingsAdapter().getItemCount() == 0) {
                View noContent  = view.getParentActivity().findViewById(R.id.no_content_detail);
                if(noContent != null)
                    noContent.setVisibility(View.VISIBLE);
            }
        }

        view.clearSelectedMode();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                .title(R.string.information_title)
                .content(R.string.assign_to_contact_ok)
                .positiveText(android.R.string.ok)
                .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.success));
        builder.show();

        if(!view.isSinglePaneLayout()) {
            ContactsListFragment listFragment = (ContactsListFragment)
                    view.getParentActivity().getSupportFragmentManager().findFragmentById(R.id.contacts_list_fragment_container);
            if(listFragment != null) {
                listFragment.setNewAddedContactId(contactId);
                listFragment.onResume();
            }
        }
    }

    public void assignToPrivate(List<Recording> recordings) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id;

        Cursor cursor = db.query(ContactsContract.Contacts.TABLE_NAME, new String[]{ContactsContract.Contacts._ID},
                ContactsContract.Contacts.COLUMN_NAME_PRIVATE_NUMBER + "=" + CrApp.SQLITE_TRUE, null, null, null, null);

        if (cursor.moveToFirst())
            id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        else {
            Contact contact = new Contact();
            contact.setPrivateNumber(true);
            try {
                contact.insertInDatabase(CrApp.getInstance());
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
                MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                        .title(R.string.error_title)
                        .content(R.string.assign_to_contact_err)
                        .positiveText(android.R.string.ok)
                        .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.error));
                builder.show();
                return;
            }
            id = contact.getId();
        }
        cursor.close();

        for (Recording recording : recordings) {
            recording.setContactId(id);
            try {
                recording.updateRecording(CrApp.getInstance());
            } catch (SQLException exc) {
                CrLog.log(CrLog.ERROR, exc.getMessage());
                MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                        .title(R.string.error_title)
                        .content(R.string.assign_to_contact_err)
                        .positiveText(android.R.string.ok)
                        .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.error));
                builder.show();
                return;
            }
            view.getRecordingsAdapter().removeItem(recording);
            if(view.getRecordingsAdapter().getItemCount() == 0) {
                View noContent  = view.getParentActivity().findViewById(R.id.no_content_detail);
                if(noContent != null)
                    noContent.setVisibility(View.VISIBLE);
            }
        }
        view.clearSelectedMode();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(view.getParentActivity())
                .title(R.string.information_title)
                .content(R.string.assign_to_contact_ok)
                .positiveText(android.R.string.ok)
                .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.success));
        builder.show();

        if(!view.isSinglePaneLayout()) {
            ContactsListFragment listFragment = (ContactsListFragment)
                    view.getParentActivity().getSupportFragmentManager().findFragmentById(R.id.contacts_list_fragment_container);
            if(listFragment != null) {  //e null dacă e apelat din unassigned
//                listFragment.setNewAddedContactId(id);
                //Instrucțiunea de mai sus are rolul să seteze contactul activ, al cărui detaliu va fi încărcat.
                //Am fost nevoit să renunț la această funcționalitate cînd se asignează un recording contactului
                //privat deoarece după setarea detaliului, în mod inexplicabil, butoanele call și edit rămîn vizibile
                //și dacă se apasă pe ele aplicația crapă. Dacă se selectează un recording devin vizibile toate
                // 6 butoane. Bugul se manifestă numai aici. Cînd se clichează pe contactul privat nu se întîmplă
                // chestia asta. Nu am putut repara acest bug.
                listFragment.onResume();
            }
        }
    }
}
