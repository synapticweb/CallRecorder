package net.synapticweb.callrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsContract.*;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;

import java.util.ArrayList;
import java.util.List;


public class PhoneNumberDetail extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    Intent intent;
    TextView typePhoneView;
    TextView phoneNumberView;
    TextView recordingStatusView;
    ImageView contactPhotoView;
    PhoneNumber phoneNumber;
    private static final String TAG = "CallRecorder";
    private static final int EDIT_REQUEST_CODE = 1;

    @Override
    public void onResume() {
        super.onResume();
        paintViews();
    }

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

        RecyclerView recordings = findViewById(R.id.recordings);
        recordings.setLayoutManager(new LinearLayoutManager(this));
        recordings.setAdapter(new RecordingAdapter(getRecordings()));
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

        //workaround necesar pentru că, dacă recyclerul cu recordinguri conține imagini poza asta devine neagră.
        // Se pare că numai pe lolipop, de verificat. https://github.com/hdodenhof/CircleImageView/issues/31
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            contactPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

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
    public boolean onMenuItemClick(MenuItem item) {
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

        if(requestCode == EDIT_REQUEST_CODE) {
            Bundle extras = intent.getExtras();
            if(extras != null)
                phoneNumber = intent.getParcelableExtra("edited_number");
//            paintViews(); odată ce refac widgeturile în onResume() nu mai este nevoie de asta.
        }
    }

    private List<Recording> getRecordings() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<Recording> list =  new ArrayList<>();

        Cursor cursor = db.query(Recordings.TABLE_NAME,
            null, Recordings.COLUMN_NAME_PHONE_NUM_ID + "=" + phoneNumber.getId(), null, null, null, null);

        while(cursor.moveToNext())
        {
            Recording recording = new Recording(cursor.getLong(cursor.getColumnIndex(Recordings._ID)),
                    cursor.getString(cursor.getColumnIndex(Recordings.COLUMN_NAME_PATH)),
                    cursor.getInt(cursor.getColumnIndex(Recordings.COLUMN_NAME_INCOMING)) == 1,
                    cursor.getLong(cursor.getColumnIndex(Recordings.COLUMN_NAME_START_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(Recordings.COLUMN_NAME_END_TIMESTAMP)));
            list.add(recording);
        }
        cursor.close();
        return list;
    }

    class RecordingHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView typeOfRecording;
        TextView recordingDate, recordingLength;

        RecordingHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recording, parent, false));
            typeOfRecording = itemView.findViewById(R.id.type_of_recording);
            recordingDate = itemView.findViewById(R.id.recording_date);
            recordingLength = itemView.findViewById(R.id.recording_length);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

        }
    }

    class RecordingAdapter extends RecyclerView.Adapter<RecordingHolder> {
        List<Recording> recordings;

        RecordingAdapter(List<Recording> recordings) {
            this.recordings = recordings;
        }

        @Override
        @NonNull
        public RecordingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            return new RecordingHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingHolder holder, int position) {
            Recording recording = recordings.get(position);
            holder.typeOfRecording.setImageResource(recording.isIncoming() ? R.drawable.incoming : R.drawable.outgoing);
            holder.recordingDate.setText(recording.getDate());
            holder.recordingLength.setText(recording.getDuration());
        }

        @Override
        public int getItemCount() {
            return recordings.size();
        }

    }


}
