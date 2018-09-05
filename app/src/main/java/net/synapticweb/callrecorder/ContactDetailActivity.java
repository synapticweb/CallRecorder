package net.synapticweb.callrecorder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import java.util.ArrayList;
import java.util.List;


public class ContactDetailActivity extends AppCompatActivity
        implements PopupMenu.OnMenuItemClickListener, ContactDetailFragment.Callbacks {
    Intent intent;
    ActionBar actionBar;
    ContactDetailFragment contactDetail;
    PhoneNumber phoneNumber;
    private boolean selectMode = false;
    private List<Integer> selectedItems = new ArrayList<>();

    public boolean isSelectModeOn() {
        return selectMode;
    }

    public boolean selectedItemsContains(int position) {
         return selectedItems.contains(position);
    }

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

    public void onRecordingEdited(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        if(!selectMode)
            toolbar.setTitle(phoneNumber.getContactName());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("phoneNumber", phoneNumber);
        outState.putIntegerArrayList("selectedItems", (ArrayList<Integer>) selectedItems);
        outState.putBoolean("selectMode", selectMode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_detail_activity);
        intent = getIntent();
        if(savedInstanceState != null) {
            phoneNumber = savedInstanceState.getParcelable("phoneNumber");
            selectMode = savedInstanceState.getBoolean("selectMode");
            selectedItems = savedInstanceState.getIntegerArrayList("selectedItems");
        }
        else
            phoneNumber = intent.getExtras().getParcelable("phoneNumber");

        FragmentManager fm = getSupportFragmentManager();
        contactDetail = (ContactDetailFragment) fm.findFragmentById(R.id.contact_detail_fragment_container);
        if(contactDetail == null) {
            contactDetail = ContactDetailFragment.newInstance(phoneNumber);
            fm.beginTransaction().
                    add(R.id.contact_detail_fragment_container, contactDetail).
                    commit();
        }

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        if(!selectMode)
            toolbar.setTitle(phoneNumber.getContactName());
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        toggleSelectMode();

        final ImageButton menuButton = findViewById(R.id.phone_number_detail_menu);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getApplicationContext(), R.style.PopupMenu);
                PopupMenu popupMenu = new PopupMenu(wrapper, v);
                popupMenu.setOnMenuItemClickListener(ContactDetailActivity.this);
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
                new MaterialDialog.Builder(ContactDetailActivity.this)
                        .title(R.string.delete_recording_confirm_title)
                        .content(String.format(getResources().getString(
                                R.string.delete_recording_confirm_message), selectedItems.size()))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .icon(ContactDetailActivity.this.getResources().getDrawable(R.drawable.warning))
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
                        .withActivity(ContactDetailActivity.this)
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

    private void toggleSelectMode() {
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

    public void clearSelectMode() {
        selectMode = false;
        toggleSelectMode();
        contactDetail.clearSelected(selectedItems);
        selectedItems.clear();
    }

}
