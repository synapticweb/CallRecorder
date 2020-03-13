package net.synapticweb.callrecorder.contactslist;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
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
    protected void toggleTitle() {
        TextView title = parentActivity.findViewById(R.id.actionbar_title);
        Toolbar.LayoutParams params = (Toolbar.LayoutParams) title.getLayoutParams();
        params.gravity = selectMode ? Gravity.START : Gravity.CENTER;
        title.setLayoutParams(params);
        title.setText(selectMode ? String.valueOf(selectedItems.size()) : getString(R.string.app_name));
    }

    @Override
    public void toggleSelectModeActionBar(boolean animate) {
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        ImageButton menuRightSelectedBtn = parentActivity.findViewById(R.id.contact_detail_selected_menu);
        ImageButton hamburger = parentActivity.findViewById(R.id.hamburger);
        toggleTitle();

        if(parentActivity.getLayoutType() == BaseActivity.LayoutType.DOUBLE_PANE && selectMode) {
            ImageButton editBtn = parentActivity.findViewById(R.id.edit_contact);
            ImageButton callBtn = parentActivity.findViewById(R.id.call_contact);
            ImageButton menuRightBtn = parentActivity.findViewById(R.id.contact_detail_menu);
            hideView(editBtn, animate);
            hideView(callBtn, animate);
            hideView(menuRightBtn, animate);
        }

        if(selectMode) showView(closeBtn, animate); else hideView(closeBtn, animate);
        if(selectMode) showView(moveBtn, animate); else hideView(moveBtn, animate);

        if(selectMode) {
            if(checkIfSelectedRecordingsDeleted())
                disableMoveBtn();
            else
                enableMoveBtn();
        }
        if(selectMode) showView(selectAllBtn, animate); else hideView(selectAllBtn, animate);
        if(selectMode) showView(infoBtn, animate); else hideView(infoBtn, animate);
        if(selectMode) showView(menuRightSelectedBtn, animate); else hideView(menuRightSelectedBtn, animate);
        if(selectMode) hideView(hamburger, animate); else showView(hamburger, animate);
    }

    @Override
    public void paintViews(List<Recording> recordings) {
        if(selectMode)
            putInSelectMode(false);
        TextView noContent = rootView.findViewById(R.id.no_content_detail);
        adapter.replaceData(recordings);
        noContent.setVisibility(recordings.size() > 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void setDetailsButtonsListeners() {
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener((View v) -> clearSelectMode());

        final ImageButton menuButtonSelected = parentActivity.findViewById(R.id.contact_detail_selected_menu);
        menuButtonSelected.setOnClickListener((View view) -> {
                PopupMenu popupMenu = new PopupMenu(parentActivity,view);
                popupMenu.setOnMenuItemClickListener((MenuItem item) -> {
                        switch (item.getItemId()) {
                            case R.id.rename_recording:
                                onRenameRecording();
                                return true;
                            case R.id.delete_recording:
                                onDeleteSelectedRecordings();
                                return true;
                            case R.id.assign_to_contact:
                                Intent pickNumber = new Intent(Intent.ACTION_PICK,
                                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                                startActivityForResult(pickNumber, REQUEST_PICK_NUMBER);
                                return true;
                            case R.id.assign_private:
                                onAssignToPrivate();
                                return true;
                            default:
                                return false;
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
            });

        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        registerForContextMenu(moveBtn);
        //foarte necesar. Altfel meniul contextual va fi arătat numai la long click.
        moveBtn.setOnClickListener(View::showContextMenu);

        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        selectAllBtn.setOnClickListener((View v) -> onSelectAll() );

        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        infoBtn.setOnClickListener((View view) -> onRecordingInfo());
    }
}
