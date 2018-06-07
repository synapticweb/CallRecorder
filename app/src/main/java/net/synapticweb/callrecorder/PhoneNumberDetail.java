package net.synapticweb.callrecorder;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PhoneNumberDetail extends AppCompatActivity {
    LinearLayout syncContact;
    Intent intent;
    boolean hasSyncedContact;
    TextView contactNameView;
    TextView typePhoneView;
    TextView phoneNumberView;
    ImageView contactPhotoView;
    PhoneNumber phoneNumber;
    static final int PICK_EXISTING_NUMBER = 1;
    private static String TAG = "CallRecorder";

    //populează view-urile din activitatea phonenumberdetail cu ajutorul cîmpurilor din obiectul PhoneNumber:
    private void repaintViews(){
        contactNameView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_contactname_intro), phoneNumber.getContactName())));
        typePhoneView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonetype_intro), phoneNumber.getPhoneType())));
        phoneNumberView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonenumber_intro), phoneNumber.getPhoneNumber())));

        if(phoneNumber.getPhotoUri() != null)
            contactPhotoView.setImageURI(phoneNumber.getPhotoUri());
        else {
            if(phoneNumber.isUnknownPhone())
                contactPhotoView.setImageResource(R.drawable.user_contact_red);
            else if(phoneNumber.isPrivateNumber())
                contactPhotoView.setImageResource(R.drawable.user_contact_yellow);
            else
                contactPhotoView.setImageResource(R.drawable.user_contact_blue);
        }

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(hasSyncedContact)
        {
            if(phoneNumber.searchContactData(phoneNumber.getPhoneNumber()) == PhoneNumber.FOUND_CONTACT) {
                this.repaintViews();
                phoneNumber.toggleUnknownFlag(false, null);
                syncContact.setVisibility(View.GONE);
            }
            hasSyncedContact = false;
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        intent = getIntent();
        //este refăcut obiectul PhoneNumber cu ajutorul date transmise în intent:
        phoneNumber = new PhoneNumber(getApplicationContext());
        phoneNumber.setContactName(intent.getStringExtra("contact_name"));
        phoneNumber.setPhoneNumber(intent.getStringExtra("phone_number"));
        phoneNumber.setPhoneType(intent.getStringExtra("phone_type"));
        phoneNumber.setPhotoUri( (intent.getStringExtra("contact_photo_uri") != null)
        ? Uri.parse(intent.getStringExtra("contact_photo_uri")) : null);
        phoneNumber.setUnknownPhone(intent.getBooleanExtra("unknown_phone", false));
        phoneNumber.setPrivateNumber(intent.getBooleanExtra("private_number", false));


        syncContact = findViewById(R.id.sync_contact); //butonul de sync devine vizibil doar cînd avem un telefon unoknown:
        if(phoneNumber.isUnknownPhone()) {
            //înainte de a face vizibile butoanele de sync mai verificăm o dată dacă nu cumva nr a fost introdus în contacte:
            if (phoneNumber.searchContactData(intent.getStringExtra("phone_number")) == PhoneNumber.NOTFOUND_CONTACT)
                syncContact.setVisibility(View.VISIBLE);
        }

        contactNameView = findViewById(R.id.contact_name_detail);
        typePhoneView = findViewById(R.id.phone_type_detail);
        phoneNumberView = findViewById(R.id.phone_number_detail);
        contactPhotoView = findViewById(R.id.contact_photo_detail);

        this.repaintViews();

        TextView syncNewContact = findViewById(R.id.sync_new_contact);
        TextView syncAssignExisting = findViewById(R.id.sync_assign_existing);

        syncNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber.getPhoneNumber());
                hasSyncedContact = true;
                startActivity(intent);
            }
        });

        syncAssignExisting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickNumber = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(pickNumber, PICK_EXISTING_NUMBER);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String number = null;
        if(requestCode == PICK_EXISTING_NUMBER)
        {
            if(resultCode == RESULT_OK)
            {
                Uri numberUri = data.getData();
                if(numberUri != null) {
                    Cursor cursor = getContentResolver().
                            query(numberUri, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                                    null, null, null);

                    if(cursor != null)
                    {
                        cursor.moveToFirst();
                        number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        cursor.close();
                    }
                    String oldNumber = phoneNumber.getPhoneNumber();
                    phoneNumber.searchContactData(number);
                    phoneNumber.toggleUnknownFlag(false, oldNumber);
                    this.repaintViews();
                    syncContact.setVisibility(View.GONE);
                }
            }
        }

    }

}
