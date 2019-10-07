package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.data.Recording;

import java.util.List;

public class UnassignedRecordingsFragment extends ContactDetailFragment {
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.unassigned_recordings_fragment, container, false);
        recordingsRecycler = rootView.findViewById(R.id.unassigned_recordings);
        recordingsRecycler.setLayoutManager(new LinearLayoutManager(parentActivity));
        recordingsRecycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));
        recordingsRecycler.setAdapter(adapter);
        return rootView;
    }

    @Override
    public void updateTitle() {
        TextView title = parentActivity.findViewById(R.id.actionbar_title);
        Toolbar.LayoutParams params = (Toolbar.LayoutParams) title.getLayoutParams();
        params.gravity = selectMode ? Gravity.START : Gravity.CENTER;
        title.setLayoutParams(params);
        title.setText(selectMode ? String.valueOf(selectedItems.size()) : isSinglePaneLayout() ?
                contact.getContactName() : getString(R.string.app_name));
    }

    @Override
    public void toggleSelectModeActionBar(boolean animateAplha) {
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        ImageButton menuRightSelectedBtn = parentActivity.findViewById(R.id.contact_detail_selected_menu);
        ImageButton hamburger = parentActivity.findViewById(R.id.hamburger);
        updateTitle();

        toggleView(closeBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(moveBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        if(selectMode && checkIfSelectedRecordingsDeleted())
            disableMoveBtn();
        toggleView(selectAllBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(infoBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(menuRightSelectedBtn, true, animateAplha ? null : selectMode ? 1f : 0f);
        toggleView(hamburger, false, animateAplha ? null : selectMode ? 0f : 1f);
    }

    @Override
    public void paintViews(List<Recording> recordings) {
        toggleSelectModeActionBar(false);
        TextView noContent = rootView.findViewById(R.id.no_content);
        adapter.replaceData(recordings);
        noContent.setVisibility(recordings.size() > 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void setDetailsButtonsListeners() {
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelectedMode();
            }
        });

        final ImageButton menuButtonSelected = parentActivity.findViewById(R.id.contact_detail_selected_menu);
        menuButtonSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(parentActivity,view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.rename_recording:
                                presenter.onRenameClick();
                                return true;
                            case R.id.delete_recording:
                                new MaterialDialog.Builder(parentActivity)
                                        .title(R.string.delete_recording_confirm_title)
                                        .content(String.format(getResources().getString(
                                                R.string.delete_recording_confirm_message),
                                                selectedItems.size()))
                                        .positiveText(android.R.string.ok)
                                        .negativeText(android.R.string.cancel)
                                        .icon(parentActivity.getResources().getDrawable(R.drawable.warning))
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog,
                                                                @NonNull DialogAction which) {
                                                presenter.deleteSelectedRecordings();
                                            }
                                        })
                                        .show();
                                return true;
                            case R.id.assign_to_contact:
                                Intent pickNumber = new Intent(Intent.ACTION_PICK,
                                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                                startActivityForResult(pickNumber, REQUEST_PICK_NUMBER);
                                return true;
                            case R.id.assign_private: presenter.assignToPrivate(getSelectedRecordings());
                                return true;
                            default:
                                return false;
                        }
                    }
                });

                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.recording_selected_popup, popupMenu.getMenu());
                MenuItem renameMenuItem = popupMenu.getMenu().findItem(R.id.rename_recording);
                Recording recording = ((RecordingAdapter) recordingsRecycler.getAdapter()).
                        getItem(selectedItems.get(0));
                if(selectedItems.size() > 1 || !recording.exists())
                    renameMenuItem.setEnabled(false);
                popupMenu.show();
            }
        });

        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        registerForContextMenu(moveBtn);
        //foarte necesar. Altfel meniul contextual va fi arătat numai la long click.
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.showContextMenu();
            }
        });

        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        selectAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.toggleSelectAll();
            }
        });
        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onInfoClick();
            }
        });
    }
}
