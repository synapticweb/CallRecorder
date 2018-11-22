package net.synapticweb.callrecorder.contactdetail;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.contactslist.ContactsListFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.settings.SettingsFragment;

public class ContactDetailActivity extends TemplateActivity {
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
        toolbar.setTitle(contact.getContactName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

}
