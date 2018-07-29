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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
    ActionBar actionBar;
    RecyclerView recordings;
    List<Integer> longTouchedItems = new ArrayList<>();
    List<Integer> selectedItems = new ArrayList<>();
    boolean selectMode = false;
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
        if(!selectMode) { //fără această condiție, dacă aplicația se duce în background în timp ce există recordinguri
            // selectate, la întoarcere selecția se pierde.
            Toolbar toolbar = findViewById(R.id.toolbar_detail);
            toolbar.setTitle(phoneNumber.getContactName());


            recordings = findViewById(R.id.recordings);
            recordings.setLayoutManager(new LinearLayoutManager(this));
            recordings.setAdapter(new RecordingAdapter(getRecordings()));
        }
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
        actionBar = getSupportActionBar();
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

        ImageButton closeBtn = findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelectMode();
            }
        });

    }

    private void clearSelectMode() {
        toggleSelectMode();
        for(int item : longTouchedItems)
        {
            View recordingSlot = recordings.getLayoutManager().findViewByPosition(item);
            toggleSelectItem(recordingSlot, null);
        }
        selectedItems.clear();
        longTouchedItems.clear();
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

    private void toggleSelectMode() {
        ImageButton closeBtn = findViewById(R.id.close_select_mode);
        TextView selectTitle = findViewById(R.id.actionbar_select_title);
        ImageButton exportBtn = findViewById(R.id.actionbar_select_export);
        ImageButton deleteBtn = findViewById(R.id.actionbar_select_delete);
        ImageButton menuRightBtn = findViewById(R.id.phone_number_detail_menu);

        actionBar.setDisplayHomeAsUpEnabled(selectMode);
        actionBar.setDisplayShowTitleEnabled(selectMode);

        closeBtn.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        selectTitle.setText(phoneNumber.getContactName());
        selectTitle.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        exportBtn.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        deleteBtn.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        menuRightBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);

        selectMode = !selectMode;
    }

    /*Cînd este apăsat lung un recording activitatea intră în selectMode. În select mode butonul de back din
    actionbar este înlocuit de un buton close, butonul meniu din dreapta dispare și apar 2 butoane noi: delete
    și export. Această funcționalitate este asigurată de toggleSelectMode(). Cîmpul selectMode este indicatorul.
    Deoarece nu am putut să ascund titlul generat automat de actionbar l-am scos cu setDisplayShowTitleEnabled(false)
    și am adăugat un TextView (@+id/actionbar_select_title) al cărui text afișează numele contactului. La fel am
    procedat cu butonul back: setDisplayHomeAsUpEnabled(false).
    În selectMode clickurile simple pe recordinguri au ca efect selectarea acestora.
    După intrarea în selectMode, recordingul este selectat automat cu toggleSelectIntem(). Aceasta presupune afișarea
    checkboxului, selectarea acestuia, schimbarea backgroundului în gri. toggleSelectItem ia ca parametri View-ul
    recordingului (primit în onLongClick()) - pentru că trebuie să schimbe chestii în recording și poziția recordingului
    în adapter - pentru că este salvată în cîmpul selectedItems, pentru operații ca ștergere și export. toggleSelectItem
    ia ca parametru un Integer și nu un int pentru că trebuie să accepte null. Cînd un item este selectat poziția lui
    este salvată. Dar cînd aceeași funcție e apelată de clearSelectMode(), nu mai are rost să fie scoase din
    selectedItems pozițiile și se pasează null.
    Lista selectedItems mai este accesată de clicklistenerul checkboxului. Cînd checkboxul este selectat poziția
    recordingului este salvată în listă, cînd este deselectat poziția este ștearsă din listă. Cînd a fost deselectat
    ultimul recorder, clicklistenerul amintit invocă funcția clearSelectedMode, despre care voi povesti mai jos.
    Ultimul lucru pe care îl face listenerul de longclick este să bage poziția recordingului în lista longTouchedItems,
    care ține evidența recordingurilor care au fost modificate prin long click.
    clearSelectMode este apelată la apăsarea butonului close sau cînd clicklistenerul checkboxului detectează că nu
    mai este niciun item selectat. Aceasta readuce actionbarul la starea inițială și schimbă înapoi aspectul tuturor
    itemilor care au fost selectați, prin longclick sau normal click. Ultimul lucru pe care îl face este să devalizeze
    cele 2 liste selectedItems și longTouchedItems. Distincția este necesară pentru că un item poate să fie modificat
    dar să nu mai fie în mod curent selectat.
    */
    private void toggleSelectItem(@NonNull View v, Integer position) {
        CheckBox checkBox = v.findViewById(R.id.checkbox);
        TextView date = v.findViewById(R.id.recording_date);
        ImageView image = v.findViewById(R.id.type_of_recording);
        RelativeLayout recording = v.findViewById(R.id.recording);

        checkBox.setChecked(selectMode);
        checkBox.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        //https://android--code.blogspot.com/2015/05/android-textview-layout-margin.html
        //https://stackoverflow.com/questions/35354032/how-to-set-layout-params-to-units-dp-android
        FrameLayout.LayoutParams lpTextView = (FrameLayout.LayoutParams) date.getLayoutParams();
        int marginLeftTextView = (int) getResources().getDimension(selectMode ?
                R.dimen.date_recording_left_margin_selected : R.dimen.date_recording_left_margin_unselected);
        lpTextView.setMarginStart(marginLeftTextView);
        date.setLayoutParams(lpTextView);

        FrameLayout.LayoutParams lpImage = (FrameLayout.LayoutParams) image.getLayoutParams();
        int marginStartImageView = (int) getResources().getDimension(selectMode ?
                R.dimen.type_of_recording_left_margin_selected : R.dimen.type_of_recording_left_margin_unselected);
        lpImage.setMarginStart(marginStartImageView);
        image.setLayoutParams(lpImage);

        recording.setBackgroundColor(selectMode ?
                getResources().getColor(R.color.light_gray) : getResources().getColor(android.R.color.transparent));

        if(selectMode && position != null)
            selectedItems.add(position);
    }

    class RecordingHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView typeOfRecording;
        TextView recordingDate, recordingLength;
        CheckBox checkBox;

        RecordingHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recording, parent, false));
            typeOfRecording = itemView.findViewById(R.id.type_of_recording);
            recordingDate = itemView.findViewById(R.id.recording_date);
            recordingLength = itemView.findViewById(R.id.recording_length);
            checkBox = itemView.findViewById(R.id.checkbox);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(((CheckBox) v).isChecked())
                        selectedItems.add(getAdapterPosition());
                    else {
                        selectedItems.remove(Integer.valueOf(getAdapterPosition()));
                        if(selectedItems.size() == 0)
                            clearSelectMode();
                    }

                }
            });
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
           if(!selectMode)
               toggleSelectMode();

            int position = this.getAdapterPosition();
            if(!longTouchedItems.contains(position)) { //necesar pentru că dacă se face de mai multe ori click lung
                //pe un recording se introduce poziția acestuia de mai multe ori în longTouchedItems, ceea ce creează probleme.
                toggleSelectItem(v, position);
                longTouchedItems.add(position);
            }
            return true;
        }

        @Override
        public void onClick(View v) {
            if(selectMode) {
                int position = this.getAdapterPosition();
                if(!longTouchedItems.contains(position)) {
                    toggleSelectItem(v, position);
                    longTouchedItems.add(position);
                }
            }
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
