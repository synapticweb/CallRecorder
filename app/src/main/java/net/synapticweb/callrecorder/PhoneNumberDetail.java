package net.synapticweb.callrecorder;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PhoneNumberDetail extends AppCompatActivity {
    Intent intent;
    TextView typePhoneView;
    TextView phoneNumberView;
    ImageView contactPhotoView;
    PhoneNumber phoneNumber;
    private static String TAG = "CallRecorder";

    private void paintViews(){
        typePhoneView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonetype_intro), phoneNumber.getPhoneType())));
        phoneNumberView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonenumber_intro), phoneNumber.getPhoneNumber())));

        if(phoneNumber.getPhotoUri() != null)
            contactPhotoView.setImageURI(phoneNumber.getPhotoUri());
        else {
            if(phoneNumber.isPrivateNumber())
                contactPhotoView.setImageResource(R.drawable.user_contact_yellow);
            else if(phoneNumber.isUnkownNumber())
                contactPhotoView.setImageResource(R.drawable.user_contact_red);
            else
                contactPhotoView.setImageResource(R.drawable.user_contact_blue);
        }

    }

    private Spanned getSpannedText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else
            return Html.fromHtml(text);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonenumber_detail);

        intent = getIntent();
        //este refăcut obiectul PhoneNumber cu ajutorul date transmise în intent:
        phoneNumber = new PhoneNumber();
        phoneNumber.setContactName(intent.getStringExtra("contact_name"));
        phoneNumber.setPhoneNumber(intent.getStringExtra("phone_number"));
        phoneNumber.setPhoneType(intent.getStringExtra("phone_type"));
        phoneNumber.setPhotoUri(intent.getStringExtra("contact_photo_uri"));
        phoneNumber.setPrivateNumber(intent.getBooleanExtra("private_number", false));
        phoneNumber.setUnkownNumber(intent.getBooleanExtra("unknown_number", false));


        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        toolbar.setTitle(phoneNumber.getContactName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        typePhoneView = findViewById(R.id.phone_type_detail);
        phoneNumberView = findViewById(R.id.phone_number_detail);
        contactPhotoView = findViewById(R.id.contact_photo_detail);

        this.paintViews();

    }


}
