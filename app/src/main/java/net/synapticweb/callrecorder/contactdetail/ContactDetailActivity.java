package net.synapticweb.callrecorder.contactdetail;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.data.Contact;

public class ContactDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_detail_activity);
        Intent intent = getIntent();
        Contact contact = intent.getParcelableExtra("contact");

        FragmentManager fm = getSupportFragmentManager();
        Fragment contactDetail = fm.findFragmentById(R.id.contact_detail_fragment_container);
        if(contactDetail == null) {
            contactDetail = ContactDetailFragment.newInstance(contact);
            fm.beginTransaction().
                    add(R.id.contact_detail_fragment_container, contactDetail).
                    commit();
        }

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        toolbar.setTitle(contact.getContactName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

}
