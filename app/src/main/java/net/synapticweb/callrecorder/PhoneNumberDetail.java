package net.synapticweb.callrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
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


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsContract.*;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;

import java.io.File;
import java.lang.ref.WeakReference;
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
    RecyclerView recordingsRecycler;
    List<Integer> longTouchedItems = new ArrayList<>();
    List<Integer> selectedItems = new ArrayList<>();
    MaterialDialog dialog;
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
        toggleSelectMode();

        if(!selectMode) {
            Toolbar toolbar = findViewById(R.id.toolbar_detail);
            toolbar.setTitle(phoneNumber.getContactName());
        }
            recordingsRecycler = findViewById(R.id.recordings);
            recordingsRecycler.setLayoutManager(new LinearLayoutManager(this));
            recordingsRecycler.setAdapter(new RecordingAdapter(getRecordings()));

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
        new MaterialDialog.Builder(PhoneNumberDetail.this)
                .title(R.string.delete_number_confirm_title)
                .content(R.string.delete_number_confirm_message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .icon(PhoneNumberDetail.this.getResources().getDrawable(R.drawable.warning))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            phoneNumber.delete(PhoneNumberDetail.this);
                        }
                        catch (Exception exc) {
                            Log.wtf(TAG, exc.getMessage());
                        }
                        finish();
                    }
                })
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("phoneNumber", phoneNumber);
        outState.putIntegerArrayList("longTouched", (ArrayList<Integer>) longTouchedItems);
        outState.putIntegerArrayList("selectedItems", (ArrayList<Integer>) selectedItems);
        outState.putBoolean("selectMode", selectMode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonenumber_detail);

        if(savedInstanceState != null) {
            phoneNumber = savedInstanceState.getParcelable("phoneNumber");
            selectMode = savedInstanceState.getBoolean("selectMode");
            longTouchedItems = savedInstanceState.getIntegerArrayList("longTouched");
            selectedItems = savedInstanceState.getIntegerArrayList("selectedItems");
        }

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

        ImageButton deleteRecording = findViewById(R.id.actionbar_select_delete);
        deleteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(PhoneNumberDetail.this)
                        .title(R.string.delete_recording_confirm_title)
                        .content(String.format(getResources().getString(
                                R.string.delete_recording_confirm_message), selectedItems.size()))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .icon(PhoneNumberDetail.this.getResources().getDrawable(R.drawable.warning))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                RecordingAdapter adapter = (RecordingAdapter) recordingsRecycler.getAdapter();

                                for(int position : selectedItems) {
                                    Recording recording = adapter.getItem(position);
                                    try {
                                        recording.delete(PhoneNumberDetail.this);
                                    }
                                    catch (Exception exc) {
                                        Log.wtf(TAG, exc.getMessage());
                                    }
                                }
                                clearSelectMode();
                                paintViews();
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
                        .withActivity(PhoneNumberDetail.this)
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
                        RecordingAdapter adapter = (RecordingAdapter) recordingsRecycler.getAdapter();
                        Recording[] recordingsArray = new Recording[selectedItems.size()];
                        int index = 0;
                        long totalSize = 0;

                        for(int position : selectedItems) {
                           Recording recording = adapter.getItem(position);
                           totalSize += new File(recording.getPath()).length();
                           recordingsArray[index++] = recording;
                        }
                        new ExportAsyncTask(path, totalSize, PhoneNumberDetail.this).execute(recordingsArray);
                    }
                });

            }
        });
    }
    /*Am decis să folosesc AsyncTask pentru copierea recordingurilor, astfel încît threadul UI să rămînă liber - pentru a putea
    anula taskul dacă ar fi necesar. AS m-a obligat să fac clasa ExportAsyncTask statică - cică altfel colectorul de gunoaie
    nu poate acționa asupra PhoneNumberDetail, fiindcă dacă ar fi nonstatică ar avea o referință permanentă la părinte.
    Pentru a implementa o bară de progres a fost necesar ca pusblishProgress() să fie apelată din Recording.export() și
    cîmpurile alreadyCopied și totalSize să fie accesibile din acea metodă. Pentru a realiza aceste lucruri am modificat export()
    astfel încît să primească drept parametru o referință la obiectul ExportAsyncTask (în Java obiectele sunt pasate în funcție
    ca referințe, nu ca valori - deci nu a trebuit să fac nimic suplimentar).
    Algoritmul de copiere l-am adaptat după ce am găsit aici: https://stackoverflow.com/questions/21239223/track-progress-of-copying-files
    (al doilea răspuns). În export() nu am putut apela direct publishProgress (pentru că e protected) așa că am folosit
    callPublishProgress.
    Am folosit MaterialDialog, care are o implementare frumoasă de progress dialog. Constructorul dialogului mi-a cerut o
    referință la activitate. N-am putut pune o referință în ExportAsyncTask - pentru că e statică, așa că am folosit metoda
    excelentă WeakReference. A se vedea și https://stackoverflow.com/questions/44309241/warning-this-asynctask-class-should-be-static-or-leaks-might-occur
    Dialogul l-am făcut modal (dialog.setCancelable(false)), pentru că altfel userul îl poate ascunde, dar taskul continuă în background ceea ce încetinește
    aplicația. Singura modalitate de a ascunde dialogul este butonul Cancel, care anulează taskul. De reținut faptul că apelarea
    cancel(true) nu încheie taskul - doar dă undă verde unei eventuale încheieri. Ca să se încheie este necesar ca doInBackground
    să verifice dacă s-a dat undă verde cu isCancelled și dacă da, să termine. Eu fac verificarea atît în export() cît și
    în doInBackground pentru că: dacă aș verifica numai în doInBackground terminarea s-ar produce numai după încheierea exportului
    unui fișier, și pot fi mari; dacă aș verifica numai în export() aș întrerupe exportul fișierului curent, dar doInBackground
    ar apela din nou export() și s-ar mai copia cîte 1MB din fiecare fișier rămas.
    */
    static class ExportAsyncTask extends AsyncTask<Recording, Integer, Boolean> {
        long alreadyCopied = 0;
        String path;
        long totalSize;
        WeakReference<PhoneNumberDetail> activityRef; //http://sohailaziz05.blogspot.com/2014/10/asynctask-and-context-leaking.html

        ExportAsyncTask(String foderPath, long totalSize, PhoneNumberDetail activity) {
            this.path = foderPath;
            this.totalSize = totalSize;
            activityRef = new WeakReference<>(activity);
        }

        void callPublishProgress(int progress) {
            publishProgress(progress);
        }

        @Override
        protected void onPreExecute() {
            activityRef.get().dialog = new MaterialDialog.Builder(activityRef.get())
                    .title("Progress")
                    .content("Exporting recordings...")
                    .progress(false, 100, true)
                    .negativeText("Cancel")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            cancel(true);
                        }
                    })
                    .build();
            activityRef.get().dialog.setCancelable(false);
            activityRef.get().dialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer...integers) {
            activityRef.get().dialog.setProgress(integers[0]);
        }

        @Override
        protected void onCancelled() {
            new MaterialDialog.Builder(activityRef.get())
                    .title("Warning")
                    .content("The export was canceled. Some files might be corrupted or missing.")
                    .positiveText("OK")
                    .icon(activityRef.get().getResources().getDrawable(R.drawable.warning))
                    .show();
            activityRef.get().clearSelectMode();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            activityRef.get().dialog.dismiss();
            if(result) {
                new MaterialDialog.Builder(activityRef.get())
                        .title("Success")
                        .content("The recording(s) were successfully exported.")
                        .positiveText("OK")
                        .icon(activityRef.get().getResources().getDrawable(R.drawable.success))
                        .show();
            }
            else {
                new MaterialDialog.Builder(activityRef.get())
                        .title("Error")
                        .content("An error occurred while exporting the recording(s). Some files might be corrupted or missing.")
                        .positiveText("OK")
                        .icon(activityRef.get().getResources().getDrawable(R.drawable.error))
                        .show();
            }
            activityRef.get().clearSelectMode();
        }

        @Override
        protected Boolean doInBackground(Recording...recordings) {
            for(Recording recording : recordings) {
                try {
                    recording.export(path, this, totalSize);
                    if(isCancelled())
                        break;
                }
                catch (Exception exc) {
                    Log.wtf(TAG, exc.getMessage());
                    return false;
                }
            }
            return true;
        }
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

    /*Cînd este apăsat lung un recording activitatea intră în selectMode. În select mode butonul de back din
    actionbar este înlocuit de un buton close, butonul meniu din dreapta dispare și apar 2 butoane noi: delete
    și export. Această funcționalitate este asigurată de toggleSelectMode(). Cîmpul selectMode este indicatorul.
    Deoarece nu am putut să ascund titlul generat automat de actionbar l-am scos cu setDisplayShowTitleEnabled(false)
    și am adăugat un TextView (@+id/actionbar_select_title) al cărui text afișează numele contactului. La fel am
    procedat cu butonul back: setDisplayHomeAsUpEnabled(false).
    În selectMode clickurile simple pe recordinguri au ca efect selectarea acestora.
    După intrarea în selectMode, recordingul este pregătit pentru selectare cu toggleLongPressed, ceea ce presupune
    schimbarea backgroundului, afișarea checkboxului (neselectat) și modificarea spațierii. toggleSelectItem ia ca parametru
    View-ul recordingului (primit în onLongClick()) - pentru că trebuie să schimbe chestii în recording. Apoi este selectat
    checkboxul și poziția itemului este salvată în listele longTouchedItems și selectedItems.
       Lista selectedItems mai este accesată de clicklistenerul checkboxului. Cînd checkboxul este selectat poziția
    recordingului este salvată în listă, cînd este deselectat poziția este ștearsă din listă. Cînd a fost deselectat
    ultimul recorder, clicklistenerul amintit invocă funcția clearSelectedMode.
    clearSelectMode este apelată la apăsarea butonului close sau cînd clicklistenerul checkboxului detectează că nu
    mai este niciun item selectat. Aceasta readuce actionbarul la starea inițială și schimbă înapoi aspectul tuturor
    itemilor care au fost selectați, prin longclick sau normal click. Ultimul lucru pe care îl face este să devalizeze
    cele 2 liste selectedItems și longTouchedItems. Distincția este necesară pentru că un item poate să fie modificat
    dar să nu mai fie în mod curent selectat.
    */
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


    private void clearSelectMode() {
        toggleSelectMode();
        for(int item : longTouchedItems)
        {
            View recordingSlot = recordingsRecycler.getLayoutManager().findViewByPosition(item);
            toggleLongPressed(recordingSlot);
        }
        selectedItems.clear();
        longTouchedItems.clear();
    }

    private void toggleLongPressed(@NonNull View v) {
        CheckBox checkBox = v.findViewById(R.id.checkbox);
        TextView date = v.findViewById(R.id.recording_date);
        ImageView image = v.findViewById(R.id.type_of_recording);
        RelativeLayout recording = v.findViewById(R.id.recording);

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
                toggleLongPressed(v);
                checkBox.setChecked(true);
                longTouchedItems.add(position);
                selectedItems.add(position);
            }
            return true;
        }

        @Override
        public void onClick(View v) {
            if(selectMode) {
                int position = this.getAdapterPosition();
                if(!longTouchedItems.contains(position)) {
                    toggleLongPressed(v);
                    checkBox.setChecked(true);
                    longTouchedItems.add(position);
                    selectedItems.add(position);
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

        Recording getItem(int position) {
            return recordings.get(position);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingHolder holder, int position) {
            Recording recording = recordings.get(position);
            holder.typeOfRecording.setImageResource(recording.isIncoming() ? R.drawable.incoming : R.drawable.outgoing);
            holder.recordingDate.setText(recording.getDate());
            holder.recordingLength.setText(recording.getDuration());

            //pentru situația cînd este întors ecranul
            if(longTouchedItems.contains(position))
                toggleLongPressed(holder.itemView);
            if(selectedItems.contains(position))
                holder.checkBox.setChecked(true);
        }

        @Override
        public int getItemCount() {
            return recordings.size();
        }

    }


}
