package net.synapticweb.callrecorder.contactdetail;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.CrApp;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class ContactDetailPresenter implements ContactDetailContract.ContactDetailPresenter {
    private ContactDetailContract.View view;
    static final int EDIT_REQUEST_CODE = 1;
    static final String EDIT_EXTRA_CONTACT = "edit_extra_contact";
    public static final String RECORDING_EXTRA = "recording_extra";
    private static final String TAG = "CallRecorder";

     ContactDetailPresenter(ContactDetailContract.View view) {
        this.view = view;
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
                    .title("Recordings info")
                    .content("Total size: " + AppLibrary.getFileSizeHuman(totalSize) + "\n\nTo get detailed info about a specific recording you must select only that recording.")
                    .positiveText(android.R.string.ok)
                    .show();
            return ;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(view.getParentActivity())
                .title("Recording info")
                .customView(R.layout.info_dialog, false)
                .positiveText(android.R.string.ok).build();

        final Recording recording = view.getRecordingsAdapter().getItem(view.getSelectedItems().get(0));
        final EditText nameEdit = dialog.getView().findViewById(R.id.info_name_edit);
        final String nameEditText = recording.getName() != null ? recording.getName() : "Name not set";
        nameEdit.setText(nameEditText);
        TextView size = dialog.getView().findViewById(R.id.info_size_data);
        size.setText(AppLibrary.getFileSizeHuman(recording.getSize()));

        TextView format = dialog.getView().findViewById(R.id.info_format_data);
        format.setText(recording.getHumanReadingFormat());
        TextView length = dialog.getView().findViewById(R.id.info_length_data);
        length.setText(AppLibrary.getDurationHuman(recording.getLength(), true));

        final Button changeName = dialog.getView().findViewById(R.id.info_change_name);
        final ImageButton okChange = dialog.getView().findViewById(R.id.info_name_done);
        changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameEdit.selectAll();
                nameEdit.setEnabled(true);
                InputMethodManager imm = (InputMethodManager) view.getParentActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null)
                    imm.showSoftInput(nameEdit, InputMethodManager.SHOW_IMPLICIT);
                okChange.setVisibility(View.VISIBLE);
                changeName.setEnabled(false);
            }
        });
        okChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameChanged = nameEdit.getText().toString().trim();
                nameEdit.setText(nameChanged);
                if(!nameChanged.equals(nameEditText)) {
                    recording.setName(nameChanged.length() > 0 ? nameChanged : null);
                    recording.updateRecording(view.getParentActivity());
                }
                if(nameChanged.length() == 0)
                    nameEdit.setText("Name not set");
                nameEdit.setSelection(0,0);
                nameEdit.setEnabled(false);
                okChange.setVisibility(View.GONE);
                changeName.setEnabled(true);
            }
        });
        dialog.show();
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

    @Override
    public void selectRecording(CardView card, int adapterPosition) {
         if(!view.isSelectModeOn()) {
             view.setSelectMode(true);
             view.toggleSelectModeActionBar(true);
         }
         if(!view.removeIfPresentInSelectedItems(adapterPosition)) {
             view.addToSelectedItems(adapterPosition);
             view.selectRecording(card);
         }
         else
             view.deselectRecording(card);

         if(view.isEmptySelectedItems())
             view.clearSelectedMode();
    }

    @Override
    public void startPlayerActivity(Recording recording) {
        Intent playIntent = new Intent(CrApp.getInstance(), PlayerActivity.class);
        playIntent.putExtra(RECORDING_EXTRA, recording);
        view.getParentActivity().startActivity(playIntent);
    }

    @Override
    public void deleteSelectedRecordings() {
        for(Recording recording : view.getSelectedRecordings()) {
            try {
                recording.delete(CrApp.getInstance());
            }
            catch (Exception exc) {
                Log.wtf(TAG, exc.getMessage());
            }
        }
    }

    @Override
    public void moveSelectedRecordings(String path) {
         int totalSize = 0;
         List<Recording> recordings = view.getSelectedRecordings();
         Recording[] recordingsArray = new Recording[recordings.size()];
         for(Recording recording : recordings)
             totalSize += new File(recording.getPath()).length();

        new MoveAsyncTask(path, totalSize, view.getParentActivity()).
                execute(recordings.toArray(recordingsArray));
        view.clearSelectedMode();
    }

    @Override
    public void toggleShouldRecord(Contact contact) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put(ContactsContract.Contacts.COLUMN_NAME_SHOULD_RECORD, !contact.shouldRecord());
        db.update(ContactsContract.Contacts.TABLE_NAME, values,
                ContactsContract.Contacts._ID + '=' + contact.getId(), null);
        contact.setShouldRecord(!contact.shouldRecord());
        view.setContact(contact);
        view.displayRecordingStatus();
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
            CardView selectedRecordingCard = (CardView) recordingsRecycler.getLayoutManager().findViewByPosition(position);
            if(selectedRecordingCard != null) //dacă recordingul nu este încă afișat pe ecran
                // (sunt multe recordinguri și se scrolează) atunci selectedRecording va fi null. Dar mai înainte am
                //notificat adapterul că s-a schimbat, ca să îl reconstruiască.
               view.selectRecording(selectedRecordingCard);
        }
    }
}
