package net.synapticweb.callrecorder.contactdetail;


import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import android.text.InputType;
import android.util.Log;
import android.view.View;
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
    public void onRenameClick() {
        new MaterialDialog.Builder(view.getParentActivity())
                .title("Rename this recording")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("Enter new name for recording", null, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        if(view.getSelectedItems().size() != 1) {
                            Log.wtf(TAG, "Calling onRenameClick when multiple recordings are selected");
                            return ;
                        }
                        Recording selRec = view.getSelectedRecordings().get(0); //de testat. size == 1
                        String parent = new File(selRec.getPath()).getParent();
                        String oldFileName = new File(selRec.getPath()).getName();
                        String ext = oldFileName.substring(oldFileName.length() - 3);
                        String newFileName = input + "." + ext;

                        if(new File(parent, newFileName).exists()) {
                            new MaterialDialog.Builder(view.getParentActivity())
                                    .title("Warning")
                                    .content("This file name is already used.")
                                    .positiveText("OK")
                                    .icon(CrApp.getInstance().getResources().getDrawable(R.drawable.warning))
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
                            Log.wtf(TAG, e.getMessage());
                            new MaterialDialog.Builder(view.getParentActivity())
                                    .title("Error")
                                    .content("An error ocurred while renaming the recording.")
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

        //There should be only one if we are here:
        if(view.getSelectedItems().size() != 1) {
            Log.wtf(TAG, "Calling onInfoClick when multiple recordings are selected");
            return ;
        }
        final Recording recording = view.getRecordingsAdapter().getItem(view.getSelectedItems().get(0));
        TextView size = dialog.getView().findViewById(R.id.info_size_data);
        size.setText(AppLibrary.getFileSizeHuman(recording.getSize()));

        TextView format = dialog.getView().findViewById(R.id.info_format_data);
        format.setText(recording.getHumanReadingFormat());
        TextView length = dialog.getView().findViewById(R.id.info_length_data);
        length.setText(AppLibrary.getDurationHuman(recording.getLength(), true));
        TextView path = dialog.getView().findViewById(R.id.info_path_data);
        path.setText(recording.isSavedInPrivateSpace() ? "Private application storage" : recording.getPath());

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
    public void selectRecording(android.view.View recording, int adapterPosition) {
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
         }
         else
             view.deselectRecording(recording);


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
        for(Recording recording :  view.getSelectedRecordings()) {
            try {
                recording.delete(CrApp.getInstance());
                view.getRecordingsAdapter().removeItem(recording);
            }
            catch (Exception exc) {
                Log.wtf(TAG, exc.getMessage());
            }
        }

        view.getSelectedItems().clear();
        if(view.getRecordingsAdapter().getItemCount() == 0)
            view.clearSelectedMode();
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
            View selectedRecording = recordingsRecycler.getLayoutManager().findViewByPosition(position);
            if(selectedRecording != null) //dacă recordingul nu este încă afișat pe ecran
                // (sunt multe recordinguri și se scrolează) atunci selectedRecording va fi null. Dar mai înainte am
                //notificat adapterul că s-a schimbat, ca să îl reconstruiască.
               view.selectRecording(selectedRecording);
        }
    }
}
