package net.synapticweb.callrecorder;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.databases.ListenedContract;
import net.synapticweb.callrecorder.databases.RecordingsContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;
import net.synapticweb.callrecorder.player.PlayerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ContactDetailFragment extends Fragment {
    private TextView typePhoneView, phoneNumberView, recordingStatusView;
    private ImageView contactPhotoView;
    private RecyclerView recordingsRecycler;
    private int widthCard, cardViewColumns;
    private PhoneNumber phoneNumber;
    private Callbacks callbacks;
    private static final String ARG_PHONE_NUMBER = "phone_number";
    private static final int EDIT_REQUEST_CODE = 1;
    private static final String TAG = "CallRecorder";

    public interface Callbacks {
        void onRecordingSelected(Integer position);
        void onRecordingEdited(PhoneNumber phoneNumber);
        boolean isSelectModeOn();
        boolean selectedItemsContains(int position);
        int getContainerWidth();
        void onDeleteContact();
    }

    @Override
    public void onResume(){
        super.onResume();
        paintViews();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putParcelable(ARG_PHONE_NUMBER, phoneNumber);
    }

    public static ContactDetailFragment newInstance(PhoneNumber phoneNumber) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_PHONE_NUMBER, phoneNumber);
        ContactDetailFragment fragment = new ContactDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
            phoneNumber = savedInstanceState.getParcelable(ARG_PHONE_NUMBER);
        else {
            Bundle args = getArguments();
            if (args != null)
                phoneNumber = args.getParcelable(ARG_PHONE_NUMBER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RelativeLayout detailView = (RelativeLayout) inflater.inflate(R.layout.contact_detail_fragment, container, false);
        typePhoneView = detailView.findViewById(R.id.phone_type_detail);
        phoneNumberView = detailView.findViewById(R.id.phone_number_detail);
        contactPhotoView = detailView.findViewById(R.id.contact_photo_detail);
        recordingStatusView = detailView.findViewById(R.id.recording_status);
        recordingsRecycler = detailView.findViewById(R.id.recordings);
        //workaround necesar pentru că, dacă recyclerul cu recordinguri conține imagini poza asta devine neagră.
        // Se pare că numai pe lolipop, de verificat. https://github.com/hdodenhof/CircleImageView/issues/31
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            contactPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        paintViews();
        return detailView;
    }

    private Spanned getSpannedText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else
            return Html.fromHtml(text);

    }

    private void paintViews(){
        calculateCardViewDimensions();
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
        displayRecordingStatus();

        recordingsRecycler.setLayoutManager(new GridLayoutManager(getActivity(), cardViewColumns));
        recordingsRecycler.setAdapter(new RecordingAdapter(getRecordings()));
    }

    public void deleteRecordings(List<Integer> selectedItems) {
        RecordingAdapter adapter = (RecordingAdapter) recordingsRecycler.getAdapter();
        for(int position : selectedItems) {
            Recording recording = adapter.getItem(position);
            try {
                recording.delete(getActivity());
            }
            catch (Exception exc) {
                Log.wtf(TAG, exc.getMessage());
            }
        }
        paintViews();
    }

    public void exportRecordings(List<Integer> selectedItems, String path) {
        RecordingAdapter adapter = (RecordingAdapter) recordingsRecycler.getAdapter();
        Recording[] recordingsArray = new Recording[selectedItems.size()];
        int index = 0;
        long totalSize = 0;

        for(int position : selectedItems) {
            Recording recording = adapter.getItem(position);
            totalSize += new File(recording.getPath()).length();
            recordingsArray[index++] = recording;
        }
        new ExportAsyncTask(path, totalSize, (ContactDetailActivity) getActivity()).execute(recordingsArray);
    }

    public void deletePhoneNumber() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.delete_number_confirm_title)
                .content(R.string.delete_number_confirm_message)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .icon(getResources().getDrawable(R.drawable.warning))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        try {
                            phoneNumber.delete(getActivity());
                        }
                        catch (Exception exc) {
                            Log.wtf(TAG, exc.getMessage());
                        }
                        callbacks.onDeleteContact();
                    }
                })
                .show();
    }

    public void editPhoneNumber() {
        Intent intent = new Intent(getActivity(), EditPhoneNumberActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivityForResult(intent, EDIT_REQUEST_CODE);
    }

    public void toggleShouldRecord() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getActivity());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ListenedContract.Listened.COLUMN_NAME_SHOULD_RECORD, !phoneNumber.shouldRecord());
        db.update(ListenedContract.Listened.TABLE_NAME, values,
                ListenedContract.Listened._ID + '=' + phoneNumber.getId(), null);
        phoneNumber.setShouldRecord(!phoneNumber.shouldRecord());
        displayRecordingStatus();
    }

    private void calculateCardViewDimensions() {
        int screenWidthDp = callbacks.getContainerWidth();
        final int cardMargin = 3, recyclerMargin = 5, minimumCardWidth = 100, maximumCardWidth = 250;

        int numCols = 3;
        int usableScreen = screenWidthDp - ((numCols * 2 * cardMargin) + (recyclerMargin * 2)) ;
        int widthCard = (int) Math.floor(usableScreen / numCols);
        if(widthCard < minimumCardWidth) {
            numCols = 2;
            usableScreen = screenWidthDp - ((numCols * 2 * cardMargin) + (recyclerMargin * 2)) ;
            widthCard = (int) Math.floor(usableScreen / numCols);
        }
        else if(widthCard > maximumCardWidth) {
            while(widthCard > maximumCardWidth) {
                numCols++;
                usableScreen = screenWidthDp - ((numCols * 2 * cardMargin) + (recyclerMargin * 2)) ;
                widthCard = (int) Math.floor(usableScreen / numCols);
            }
        }

        this.widthCard = widthCard;
        this.cardViewColumns = numCols;
        Log.wtf(TAG, "Window width: " + screenWidthDp + " CardWidth: " + widthCard + " Numcols: " + cardViewColumns );
    }

    private void displayRecordingStatus(){
        if(phoneNumber.shouldRecord()) {
            recordingStatusView.setText(R.string.rec_status_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            recordingStatusView.setText(R.string.rec_status_not_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.red));
        }
    }

    public void clearSelected(List<Integer> selectedItems) {
        for(int item : selectedItems) {
            CardView recordingSlot = (CardView) recordingsRecycler.getLayoutManager().findViewByPosition(item);
            if(recordingSlot != null) //este posibil ca recordingul să fi fost șters
                toggleSelected(recordingSlot);
        }
    }

    private List<Recording> getRecordings() {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(getActivity());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        List<Recording> list =  new ArrayList<>();

        Cursor cursor = db.query(RecordingsContract.Recordings.TABLE_NAME,
                null, RecordingsContract.Recordings.COLUMN_NAME_PHONE_NUM_ID + "=" + phoneNumber.getId(), null, null, null, null);

        while(cursor.moveToNext())
        {
            Recording recording = new Recording(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings._ID)),
                    cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_PATH)),
                    cursor.getInt(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_INCOMING)) == 1,
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_START_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP)));
            list.add(recording);
        }
        cursor.close();
        return list;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if (resultCode != Activity.RESULT_OK) {
            Log.wtf(TAG, "The result code is error");
            return;
        }

        if(requestCode == EDIT_REQUEST_CODE) {
            Bundle extras = intent.getExtras();
            if(extras != null) {
                phoneNumber = intent.getParcelableExtra(EditPhoneNumberActivity.EDITED_CONTACT);
                paintViews();
                callbacks.onRecordingEdited(phoneNumber);
            }
//            paintViews(); odată ce refac widgeturile în onResume() nu mai este nevoie de asta.
        }
    }

    private void toggleSelected(@NonNull CardView card) {
        ImageView selectedTick = card.findViewById(R.id.recording_selected);
        selectedTick.setVisibility((selectedTick.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
        card.setCardBackgroundColor((card.getCardBackgroundColor().getDefaultColor() == getResources().getColor(R.color.white)) ? getResources().getColor(R.color.light_gray) :
                getResources().getColor(R.color.white));
    }

    class RecordingHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView recordingDate, recordingTime;
        ImageView recordingType, soundSymbol, recordingSelected;
        final static double soundSymbolToCardRatio = 0.45;
        final static double soundSymbolHeightToWidthRatio = 0.672;
        final static double dateAndTimeMarginsToCardRatio = 0.05;
        final static double recordingTypeToCardRatio = 0.12;
        final static double recordingTypeMarginsToCardRatio = 0.04;
        final static double selectedTickToCardRatio = 0.2;
        final static double selectedTickMarginsToCardRatio = 0.03;

        private void setDimensions() {
            //dimensiunile cardView-ului, deja calculate:
            itemView.getLayoutParams().width = AppLibrary.pxFromDp(getActivity(), widthCard);
            itemView.getLayoutParams().height = AppLibrary.pxFromDp(getActivity(), widthCard);

            //dimensiunile simbolului pentru sunet:
            int soundSymbolWidth = (int) Math.floor(widthCard * soundSymbolToCardRatio);
            int soundSymbolHeight = (int) (soundSymbolHeightToWidthRatio * soundSymbolWidth);
            soundSymbol.getLayoutParams().width = AppLibrary.pxFromDp(getActivity(),soundSymbolWidth);
            soundSymbol.getLayoutParams().height = AppLibrary.pxFromDp(getActivity(),soundSymbolHeight);

            //marginile TextView-urilor cu data și ora:
            RelativeLayout.LayoutParams lpRecordingDate = (RelativeLayout.LayoutParams) recordingDate.getLayoutParams();
            lpRecordingDate.setMargins(0,
                    AppLibrary.pxFromDp(getActivity(), (int )(widthCard * dateAndTimeMarginsToCardRatio)), 0, 0);
            recordingDate.setLayoutParams(lpRecordingDate);
            RelativeLayout.LayoutParams lpRecordingTime = (RelativeLayout.LayoutParams) recordingTime.getLayoutParams();
            lpRecordingTime.setMargins(0,
                    0, 0, AppLibrary.pxFromDp(getActivity(), (int )(widthCard * dateAndTimeMarginsToCardRatio)));
            recordingTime.setLayoutParams(lpRecordingTime);

            //dimensiunile și marginile recordingType:
            RelativeLayout.LayoutParams lpRecordingType = (RelativeLayout.LayoutParams) recordingType.getLayoutParams();
            lpRecordingType.width = AppLibrary.pxFromDp(getActivity(), (int) (widthCard * recordingTypeToCardRatio));
            lpRecordingType.height = AppLibrary.pxFromDp(getActivity(), (int) (widthCard * recordingTypeToCardRatio));
            lpRecordingType.setMargins(
                    AppLibrary.pxFromDp(getActivity(), (int)(widthCard * recordingTypeMarginsToCardRatio)), 0, 0,
                    AppLibrary.pxFromDp(getActivity(), (int)(widthCard * recordingTypeMarginsToCardRatio)) );
            recordingType.setLayoutParams(lpRecordingType);

            //dimensiunile și marginile tickului selected:
            RelativeLayout.LayoutParams lpRecordingSelected = (RelativeLayout.LayoutParams) recordingSelected.getLayoutParams();
            lpRecordingSelected.width = AppLibrary.pxFromDp(getActivity(), (int) (widthCard * selectedTickToCardRatio));
            lpRecordingSelected.height = AppLibrary.pxFromDp(getActivity(), (int) (widthCard * selectedTickToCardRatio));
            lpRecordingSelected.setMargins(0,
                    AppLibrary.pxFromDp(getActivity(), (int )(widthCard * selectedTickMarginsToCardRatio)),
                    AppLibrary.pxFromDp(getActivity(), (int)(widthCard * selectedTickMarginsToCardRatio)), 0 );
            recordingSelected.setLayoutParams(lpRecordingSelected);
        }

        RecordingHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recording, parent, false));
            recordingDate = itemView.findViewById(R.id.recording_date);
            recordingTime = itemView.findViewById(R.id.recording_time);
            recordingType = itemView.findViewById(R.id.recording_type);
            soundSymbol = itemView.findViewById(R.id.sound_symbol);
            recordingSelected = itemView.findViewById(R.id.recording_selected);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            setDimensions();
        }


        @Override
        public boolean onLongClick(View v) {
            int position = this.getAdapterPosition();
            callbacks.onRecordingSelected(position);
            toggleSelected((CardView) v);
            return true;
        }

        @Override
        public void onClick(View v) {
            if(callbacks.isSelectModeOn()) {
                int position = this.getAdapterPosition();
                callbacks.onRecordingSelected(position);
                toggleSelected((CardView) v);
            }
            else { //usual short click
                Intent playIntent = new Intent(getActivity(), PlayerActivity.class);
                RecordingAdapter adapter = (RecordingAdapter) recordingsRecycler.getAdapter();
                playIntent.putExtra("recording", adapter.getItem(getAdapterPosition()));
                startActivity(playIntent);
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
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new RecordingHolder(layoutInflater, parent);
        }

        Recording getItem(int position) {
            return recordings.get(position);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingHolder holder, int position) {
            Recording recording = recordings.get(position);
            holder.recordingDate.setText(recording.getDate());
            holder.recordingTime.setText(recording.getTime());
            holder.recordingType.setImageResource(recording.isIncoming() ? R.drawable.incoming : R.drawable.outgoing);

            //pentru situația cînd este întors ecranul sau cînd activitatea trece în background:
            if(callbacks.selectedItemsContains(position))
                toggleSelected((CardView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return recordings.size();
        }

    }
}
