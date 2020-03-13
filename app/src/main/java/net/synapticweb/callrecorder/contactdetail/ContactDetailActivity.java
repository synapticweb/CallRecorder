/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.contactslist.ContactsListFragment;
import net.synapticweb.callrecorder.data.Contact;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class ContactDetailActivity extends BaseActivity {
    Contact contact;

    @Override
    protected Fragment createFragment() {
        return ContactDetailFragment.newInstance(contact);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfThemeChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setContentView(R.layout.contact_detail_activity);
        Intent intent = getIntent();
        contact = intent.getParcelableExtra(ContactsListFragment.ARG_CONTACT);

        insertFragment(R.id.contact_detail_fragment_container);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);

        TextView title = findViewById(R.id.actionbar_title);
        title.setText(contact.getContactName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }
}
