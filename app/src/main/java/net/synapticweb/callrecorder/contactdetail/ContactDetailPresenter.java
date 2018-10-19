package net.synapticweb.callrecorder.contactdetail;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.CallRecorderApplication;
import net.synapticweb.callrecorder.ExportAsyncTask;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ListenedContract;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.data.RecordingsDbHelper;
import net.synapticweb.callrecorder.data.RecordingsRepository;
import net.synapticweb.callrecorder.player.PlayerActivity;

import java.io.File;
import java.util.List;

public class ContactDetailPresenter implements ContactDetailContract.ContactDetailPresenter {
    private ContactDetailContract.View view;
    public static final int EDIT_REQUEST_CODE = 1;
    public static final String EDIT_EXTRA_CONTACT = "edit_extra_contact";
    public static final String RECORDING_EXTRA = "recording_extra";
    private static final String TAG = "CallRecorder";

     ContactDetailPresenter(ContactDetailContract.View view) {
        this.view = view;
    }

    @Override
    public void deleteContact(final Contact contact) {
        final AppCompatActivity parentActivity = view.getParentActivity();
        new MaterialDialog.Builder(parentActivity)
                .title(R.string.delete_number_confirm_title)
                .content(String.format(parentActivity.getResources().
                        getString(R.string.delete_number_confirm_message), contact.getContactName()))
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
                            Log.wtf(TAG, exc.getMessage());
                        }
                        if(!view.isSinglePaneLayout()) {
                            ContactsListFragment listFragment = (ContactsListFragment)
                                    parentActivity.getSupportFragmentManager().findFragmentById(R.id.contacts_list_fragment_container);
                            listFragment.resetDetailFragment();
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
        Intent intent = new Intent(CallRecorderApplication.getInstance(), EditPhoneNumberActivity.class);
        intent.putExtra(EDIT_EXTRA_CONTACT, contact);
        fragment.startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    @Override
    public void onEditActivityResult(Bundle result) {
        if (result != null) {
            view.setContact((Contact) result.getParcelable(EditPhoneNumberActivity.EDITED_CONTACT));
            if(view.isSinglePaneLayout())
                view.setActionBarTitleIfActivityDetail();
        }
    }

    @Override
    public void loadRecordings(Contact contact) {
        RecordingsRepository.getRecordings(contact, new RecordingsRepository.loadRecordingsCallback() {
            @Override
            public void onRecordingsLoaded(List<Recording> recordings) {
                view.toggleSelectModeActionBar();
                view.paintViews(recordings);
                //aici ar trebui să fie cod care să pună tickuri pe recordingurile selectate cînd
                //este întors device-ul. Dar dacă pun aici codul nu se execută pentru că nu vor fi gata
                //cardview-urile. Așa că acest cod se duce în RecordingAdapter::onBindViewHolder()
                // total - neintuitiv.
            }
        });
    }

    @Override
    public void selectRecording(CardView card, int adapterPosition) {
         view.toggleSelectedRecording(card);
         if(!view.getSelectMode()) {
             view.setSelectMode(true);
             view.toggleSelectModeActionBar();
         }
         if(!view.removeIfPresentInSelectedItems(adapterPosition))
             view.addToSelectedItems(adapterPosition);

         if(view.isEmptySelectedItems())
             view.clearSelectedMode();
    }

    @Override
    public void startPlayerActivity(Recording recording) {
        Intent playIntent = new Intent(CallRecorderApplication.getInstance(), PlayerActivity.class);
        playIntent.putExtra(RECORDING_EXTRA, recording);
        view.getParentActivity().startActivity(playIntent);
    }

    @Override
    public void deleteSelectedRecordings() {
        for(Recording recording : view.getSelectedRecordings()) {
            try {
                recording.delete(CallRecorderApplication.getInstance());
            }
            catch (Exception exc) {
                Log.wtf(TAG, exc.getMessage());
            }
        }
    }

    @Override
    public void exportSelectedRecordings(String path) {
         int totalSize = 0;
         List<Recording> recordings = view.getSelectedRecordings();
         Recording[] recordingsArray = new Recording[recordings.size()];
         for(Recording recording : recordings)
             totalSize += new File(recording.getPath()).length();

        new ExportAsyncTask(path, totalSize, view.getParentActivity()).
                execute(recordings.toArray(recordingsArray));
        view.clearSelectedMode();
    }

    @Override
    public void toggleShouldRecord(Contact contact) {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(CallRecorderApplication.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD, !contact.shouldRecord());
        db.update(ListenedContract.Listened.TABLE_NAME, values,
                ListenedContract.Listened._ID + '=' + contact.getId(), null);
        contact.setShouldRecord(!contact.shouldRecord());
        view.setContact(contact);
        view.displayRecordingStatus();
    }
}
