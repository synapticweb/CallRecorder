package net.synapticweb.callrecorder;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import java.util.ArrayList;
import net.synapticweb.callrecorder.AppLibrary.*;

public class ContactDetailActivity extends HandleDetailActivity {
    Intent intent;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("phoneNumber", phoneNumber);
        outState.putIntegerArrayList("selectedItems", (ArrayList<Integer>) selectedItems);
        outState.putBoolean("selectMode", selectMode);
    }

    @Override
    @HandleDetailFragment
    public void onDeleteContact() {
        finish();
    }

    @Override
    @HandleDetailFragment
    public void onRecordingEdited(PhoneNumber phoneNumber) {
        super.onRecordingEdited(phoneNumber);
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        if(!selectMode)
            toolbar.setTitle(phoneNumber.getContactName());
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
        setDetailButtonListeners();
    }

}
