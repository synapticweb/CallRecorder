package net.synapticweb.callrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;


public class PhoneNumberDetail extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    Intent intent;
    TextView typePhoneView;
    TextView phoneNumberView;
    TextView recordingStatusView;
    ImageView contactPhotoView;
    PhoneNumber phoneNumber;
    private static final String TAG = "CallRecorder";
    private static final int EDIT_REQUEST_CODE = 1;

    private void paintViews(){
        typePhoneView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonetype_intro), phoneNumber.getPhoneTypeName())));
        phoneNumberView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonenumber_intro), phoneNumber.getPhoneNumber())));

        if(phoneNumber.getPhotoUri() != null) {
            contactPhotoView.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhotoView.setImageURI(phoneNumber.getPhotoUri());
        }
        else {
            if(phoneNumber.isPrivateNumber())
                contactPhotoView.setImageResource(R.drawable.user_contact_yellow);
            else if(phoneNumber.isUnkownNumber())
                contactPhotoView.setImageResource(R.drawable.user_contact_red);
            else
                contactPhotoView.setImageResource(R.drawable.user_contact_blue);
        }
        toggleRecordingStatus();
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        toolbar.setTitle(phoneNumber.getContactName());
    }

    private void toggleRecordingStatus(){
        if(phoneNumber.shouldRecord()) {
            recordingStatusView.setText(R.string.rec_status_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            recordingStatusView.setText(R.string.rec_status_not_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.red));
        }
    }

    private Spanned getSpannedText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else
            return Html.fromHtml(text);

    }

    private void deletePhoneNumber() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_number_confirm_title)
                .setMessage(R.string.delete_number_confirm_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                           phoneNumber.delete(PhoneNumberDetail.this);
                        }
                        catch (Exception exc) {
                            Log.wtf(TAG, exc.getMessage());
                        }
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toggleShouldRecord() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(this);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD, !phoneNumber.shouldRecord());
        db.update(ListenedContract.Listened.TABLE_NAME, values,
                ListenedContract.Listened._ID + '=' + phoneNumber.getId(), null);
        phoneNumber.setShouldRecord(!phoneNumber.shouldRecord());
        toggleRecordingStatus();
    }

    private void editPhoneNumber() {
        Intent intent = new Intent(PhoneNumberDetail.this, EditPhoneNumberActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonenumber_detail);

        intent = getIntent();
        phoneNumber = intent.getExtras().getParcelable("phoneNumber");

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        toolbar.setTitle(phoneNumber.getContactName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        typePhoneView = findViewById(R.id.phone_type_detail);
        phoneNumberView = findViewById(R.id.phone_number_detail);
        contactPhotoView = findViewById(R.id.contact_photo_detail);
        recordingStatusView = findViewById(R.id.recording_status);

        this.paintViews();

        final ImageButton menuButton = findViewById(R.id.phone_number_detail_menu);

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(getApplicationContext(), R.style.PopupMenu);
                PopupMenu popupMenu = new PopupMenu(wrapper, v);
                popupMenu.setOnMenuItemClickListener(PhoneNumberDetail.this);
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

    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.delete_phone_number:
                deletePhoneNumber();
                return true;
            case R.id.edit_phone_number:
                editPhoneNumber();
                return true;
            case R.id.should_record:
                toggleShouldRecord();
            default:
                return false;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        if (resultCode != RESULT_OK) {
            Log.wtf(TAG, "The result code is error");
            return;
        }

        if(requestCode == EDIT_REQUEST_CODE)
        {
            Bundle extras = intent.getExtras();
            if(extras != null)
                phoneNumber = intent.getParcelableExtra("edited_number");
            paintViews();
        }
    }


}
