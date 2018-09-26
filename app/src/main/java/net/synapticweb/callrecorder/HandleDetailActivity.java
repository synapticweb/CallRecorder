package net.synapticweb.callrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import java.util.ArrayList;
import java.util.List;
import net.synapticweb.callrecorder.AppLibrary.*;

//Această clasă conține cod comun pentru ContactDetailActivity și ContactsListActivityMain. Codul se ocupă de toolbar
//cînd este selectat/deselectat un recording.
abstract class HandleDetailActivity extends AppCompatActivity implements ContactDetailFragment.Callbacks, PopupMenu.OnMenuItemClickListener {
    protected PhoneNumber phoneNumber;
    protected boolean selectMode = false;
    protected List<Integer> selectedItems = new ArrayList<>();
    protected ContactDetailFragment contactDetail;
    protected ActionBar actionBar;

    //întoarce lățimea în dp a containerului care conține fragmentul cu detalii.
    @Override
    @HandleDetailFragment
    public int getContainerWidth() {
        Configuration configuration = getResources().getConfiguration();
        if(findViewById(R.id.contacts_list_fragment_container) != null &&
                findViewById(R.id.contact_detail_fragment_container) != null) { //suntem pe tabletă
            return configuration.screenWidthDp / 2;
        }
        else
            //https://stackoverflow.com/questions/6465680/how-to-determine-the-screen-width-in-terms-of-dp-or-dip-at-runtime-in-android
            return configuration.screenWidthDp;

    }

    @Override
    @HandleDetailFragment
    public boolean isSelectModeOn() {
        return selectMode;
    }

    @Override
    @HandleDetailFragment
    public boolean selectedItemsContains(int position) {
        return selectedItems.contains(position);
    }

    @Override
    @HandleDetailFragment
    public void onRecordingSelected(Integer position) {
        if(!selectMode) {
            selectMode = true;
            toggleSelectMode();
        }
        if (selectedItems.contains(position))
            selectedItems.remove(position);
        else
            selectedItems.add(position);
        if(selectedItems.isEmpty())
            clearSelectMode();
    }

    @Override
    @HandleDetailFragment
    public void onRecordingEdited(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    //necesar pentru că altfel nu apelează onActivityResult din fragmente:
    // https://stackoverflow.com/questions/6147884/onactivityresult-is-not-being-called-in-fragment
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }

    protected void toggleSelectMode() {
        ImageButton closeBtn = findViewById(R.id.close_select_mode);
        TextView selectTitle = findViewById(R.id.actionbar_select_title);
        ImageButton exportBtn = findViewById(R.id.actionbar_select_export);
        ImageButton deleteBtn = findViewById(R.id.actionbar_select_delete);
        ImageButton menuRightBtn = findViewById(R.id.phone_number_detail_menu);

        actionBar.setDisplayHomeAsUpEnabled(!selectMode);
        actionBar.setDisplayShowTitleEnabled(!selectMode);

        closeBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        selectTitle.setText(phoneNumber.getContactName());
        selectTitle.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        exportBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        deleteBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        menuRightBtn.setVisibility(selectMode ? View.GONE : View.VISIBLE);
    }

    protected void clearSelectMode() {
        selectMode = false;
        toggleSelectMode();
        contactDetail.clearSelected(selectedItems);
        selectedItems.clear();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.delete_phone_number:
                contactDetail.deletePhoneNumber();
                return true;
            case R.id.edit_phone_number:
                contactDetail.editPhoneNumber();
                return true;
            case R.id.should_record:
                contactDetail.toggleShouldRecord();
            default:
                return false;
        }
    }

    protected void setDetailButtonListeners() {
        final ImageButton menuButton = findViewById(R.id.phone_number_detail_menu);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getApplicationContext(), R.style.PopupMenu);
                PopupMenu popupMenu = new PopupMenu(wrapper, v);
                popupMenu.setOnMenuItemClickListener(HandleDetailActivity.this);
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.phone_number_popup, popupMenu.getMenu());
                MenuItem shouldRecordMenuItem = popupMenu.getMenu().findItem(R.id.should_record);
                if(phoneNumber.shouldRecord())
                    shouldRecordMenuItem.setTitle(R.string.stop_recording);
                else
                    shouldRecordMenuItem.setTitle(R.string.start_recording);
                popupMenu.show();
            }
        });

        ImageButton closeBtn = findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelectMode();
            }
        });

        ImageButton deleteRecording = findViewById(R.id.actionbar_select_delete);
        deleteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(HandleDetailActivity.this)
                        .title(R.string.delete_recording_confirm_title)
                        .content(String.format(getResources().getString(
                                R.string.delete_recording_confirm_message), selectedItems.size()))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .icon(HandleDetailActivity.this.getResources().getDrawable(R.drawable.warning))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                contactDetail.deleteRecordings(selectedItems);
                                clearSelectMode();
                            }
                        })
                        .show();
            }
        });

        ImageButton exportBtn = findViewById(R.id.actionbar_select_export);
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Content content = new Content();
                content.setOverviewHeading(getResources().getString(R.string.export_heading));

                StorageChooser chooser = new StorageChooser.Builder()
                        .withActivity(HandleDetailActivity.this)
                        .withFragmentManager(getFragmentManager())
                        .allowCustomPath(true)
                        .setType(StorageChooser.DIRECTORY_CHOOSER)
                        .withMemoryBar(true)
                        .allowAddFolder(true)
                        .showHidden(true)
                        .withContent(content)
                        .build();

                chooser.show();

                chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String path) {
                        contactDetail.exportRecordings(selectedItems, path);
                    }
                });

            }
        });

    }

}
