package net.synapticweb.callrecorder.contactdetail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;

import java.util.ArrayList;
import java.util.List;


public class ContactDetailFragment extends Fragment implements ContactDetailContract.View{
    private ContactDetailPresenter presenter;
    private RecordingAdapter adapter;
    private TextView typePhoneView, phoneNumberView, recordingStatusView;
    private ImageView contactPhotoView;
    private RecyclerView recordingsRecycler;
    private RelativeLayout detailView;
    private int widthCard, cardViewColumns;
    private Contact contact;
    private boolean selectMode = false;
    private List<Integer> selectedItems = new ArrayList<>();
    private AppCompatActivity parentActivity;
    private static final String ARG_CONTACT = "arg_contact";
    private static final String SELECT_MODE_KEY = "select_mode_key";
    private static final String SELECTED_ITEMS_KEY = "selected_items_key";
    private static final String TAG = "CallRecorder";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parentActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parentActivity = null;
    }

    @Override
    public void setActionBarTitleIfActivityDetail() {
        ActionBar actionBar = parentActivity.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(contact.getContactName());
        }
    }

    @Override
    public AppCompatActivity getParentActivity() {
        return parentActivity;
    }

    @Override
    public List<Recording> getSelectedRecordings() {
        List<Recording> list = new ArrayList<>();
        for(int adapterPosition : selectedItems)
            list.add(adapter.getItem(adapterPosition));
        return list;
    }

    @Override
    public void setSelectMode(boolean isSelectModeOn) {
        this.selectMode = isSelectModeOn;
    }

    @Override
    public boolean isEmptySelectedItems() {
        return selectedItems.isEmpty();
    }

    @Override
    public boolean getSelectMode() {
        return selectMode;
    }

    @Override
    public void addToSelectedItems(int adapterPosition) {
        selectedItems.add(adapterPosition);
    }

    @Override
    public boolean removeIfPresentInSelectedItems(int adapterPosition) {
        if (selectedItems.contains(adapterPosition)) {
            selectedItems.remove((Integer) adapterPosition); //fără casting îl interpretează ca poziție
            //în selectedItems
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean isSinglePaneLayout() {
        return (parentActivity != null &&
                parentActivity.findViewById(R.id.contacts_list_fragment_container) == null);
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void onResume(){
        super.onResume();
        presenter.loadRecordings(contact);
    }

    @Override
    public void toggleSelectModeActionBar() {
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        TextView selectTitle = parentActivity.findViewById(R.id.actionbar_select_title);
        ImageButton exportBtn = parentActivity.findViewById(R.id.actionbar_select_export);
        ImageButton deleteBtn = parentActivity.findViewById(R.id.actionbar_select_delete);
        ImageButton menuRightBtn = parentActivity.findViewById(R.id.phone_number_detail_menu);
        ActionBar actionBar = parentActivity.getSupportActionBar();

        if(actionBar != null && isSinglePaneLayout()) {
            actionBar.setDisplayHomeAsUpEnabled(!selectMode);
            actionBar.setDisplayShowTitleEnabled(!selectMode);
        }

        closeBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        if(isSinglePaneLayout()) {
            selectTitle.setText(contact.getContactName());
            selectTitle.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        }

        exportBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        deleteBtn.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        menuRightBtn.setVisibility(selectMode ? View.GONE : View.VISIBLE);

        if(!isSinglePaneLayout()) {
            Button hamburger = parentActivity.findViewById(R.id.hamburger);
            hamburger.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void clearSelectedMode() {
        selectMode = false;
        toggleSelectModeActionBar();
        toggleSelectedMultipleRecordings();
        selectedItems.clear();
    }

    @Override
    public void toggleSelectedMultipleRecordings() {
        for (int adapterPosition : selectedItems) {
            CardView recordingSlot = (CardView) recordingsRecycler.getLayoutManager().findViewByPosition(adapterPosition);
            if (recordingSlot != null) //este posibil ca recordingul să fi fost șters
                toggleSelectedRecording(recordingSlot);
        }
    }

    @Override
    public void toggleSelectedRecording(@NonNull CardView card) {
        ImageView selectedTick = card.findViewById(R.id.recording_selected);
        selectedTick.setVisibility((selectedTick.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
        card.setCardBackgroundColor((card.getCardBackgroundColor().getDefaultColor() == getResources().getColor(R.color.white)) ? getResources().getColor(R.color.light_gray) :
                getResources().getColor(R.color.white));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDetailsButtonsListeners();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(SELECT_MODE_KEY, selectMode);
        outState.putIntegerArrayList(SELECTED_ITEMS_KEY, (ArrayList<Integer>) selectedItems);
    }

    public static ContactDetailFragment newInstance(Contact contact) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONTACT, contact);
        ContactDetailFragment fragment = new ContactDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void setDetailsButtonsListeners() {
        final ImageButton menuButton = parentActivity.findViewById(R.id.phone_number_detail_menu);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context wrapper = new ContextThemeWrapper(parentActivity, R.style.PopupMenu);
                PopupMenu popupMenu = new PopupMenu(wrapper, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId())
                        {
                            case R.id.delete_phone_number:
                                presenter.deleteContact(contact);
                                return true;
                            case R.id.edit_phone_number:
                                presenter.editContact(contact);
                                return true;
                            case R.id.should_record:
                                presenter.toggleShouldRecord(contact);
                            default:
                                return false;
                        }
                    }
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.phone_number_popup, popupMenu.getMenu());
                MenuItem shouldRecordMenuItem = popupMenu.getMenu().findItem(R.id.should_record);
                if(contact.shouldRecord())
                    shouldRecordMenuItem.setTitle(R.string.stop_recording);
                else
                    shouldRecordMenuItem.setTitle(R.string.start_recording);
                MenuItem editMenuItem = popupMenu.getMenu().findItem(R.id.edit_phone_number);
                if(contact.isPrivateNumber()) {
                    editMenuItem.setEnabled(false);
                    shouldRecordMenuItem.setEnabled(false);
                }
                popupMenu.show();
            }
        });

        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearSelectedMode();
            }
        });

        ImageButton deleteRecording = parentActivity.findViewById(R.id.actionbar_select_delete);
        deleteRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(parentActivity)
                        .title(R.string.delete_recording_confirm_title)
                        .content(String.format(getResources().getString(
                                R.string.delete_recording_confirm_message), selectedItems.size()))
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .icon(parentActivity.getResources().getDrawable(R.drawable.warning))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                presenter.deleteSelectedRecordings();
                                presenter.loadRecordings(contact);
                                clearSelectedMode();
                            }
                        })
                        .show();
            }
        });

        ImageButton exportBtn = parentActivity.findViewById(R.id.actionbar_select_export);
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Content content = new Content();
                content.setOverviewHeading(getResources().getString(R.string.export_heading));

                StorageChooser chooser = new StorageChooser.Builder()
                        .withActivity(parentActivity)
                        .withFragmentManager(parentActivity.getFragmentManager())
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
                        presenter.exportSelectedRecordings(path);
                    }
                });

            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new ContactDetailPresenter(this);
        adapter = new RecordingAdapter(new ArrayList<Recording>(0));
        Bundle args = getArguments();
        if(args != null)
            contact = args.getParcelable(ARG_CONTACT);

        if(savedInstanceState != null) {
            selectMode = savedInstanceState.getBoolean(SELECT_MODE_KEY);
            selectedItems = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS_KEY);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        detailView = (RelativeLayout) inflater.inflate(R.layout.contact_detail_fragment, container, false);
        typePhoneView = detailView.findViewById(R.id.phone_type_detail);
        phoneNumberView = detailView.findViewById(R.id.phone_number_detail);
        contactPhotoView = detailView.findViewById(R.id.contact_photo_detail);
        recordingStatusView = detailView.findViewById(R.id.recording_status);
        recordingsRecycler = detailView.findViewById(R.id.recordings);
        //workaround necesar pentru că, dacă recyclerul cu recordinguri conține imagini poza asta devine neagră.
        // Se pare că numai pe lolipop, de verificat. https://github.com/hdodenhof/CircleImageView/issues/31
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            contactPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        calculateCardViewDimensions();
        recordingsRecycler.setLayoutManager(new GridLayoutManager(parentActivity, cardViewColumns));
        recordingsRecycler.setAdapter(adapter);

        return detailView;
    }

    private Spanned getSpannedText(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else
            return Html.fromHtml(text);

    }

    @Override
    public void paintViews(List<Recording> recordings){
        typePhoneView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonetype_intro), contact.getPhoneTypeName())));
        phoneNumberView.setText(getSpannedText(String.format(getResources().getString(
                R.string.detail_phonenumber_intro), contact.getPhoneNumber())));

        if(contact.getPhotoUri() != null) {
            contactPhotoView.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhotoView.setImageURI(contact.getPhotoUri());
        }
        else {
            if(contact.isPrivateNumber())
                contactPhotoView.setImageResource(R.drawable.user_contact_red);
            else
                contactPhotoView.setImageResource(R.drawable.user_contact_blue);
        }
        displayRecordingStatus();

        TextView noContent = detailView.findViewById(R.id.no_content);
        adapter.replaceData(recordings);

        if(recordings.size() > 0)
            noContent.setVisibility(View.GONE);
        else
            noContent.setVisibility(View.VISIBLE);
    }

    //întoarce lățimea în dp a containerului care conține fragmentul cu detalii.
    private int getContainerWidth() {
        Configuration configuration = getResources().getConfiguration();
        if(parentActivity.findViewById(R.id.contacts_list_fragment_container) != null &&
                parentActivity.findViewById(R.id.contact_detail_fragment_container) != null) { //suntem pe tabletă
            return configuration.screenWidthDp / 2;
        }
        else
            //https://stackoverflow.com/questions/6465680/how-to-determine-the-screen-width-in-terms-of-dp-or-dip-at-runtime-in-android
            return configuration.screenWidthDp;

    }

    private void calculateCardViewDimensions() {
        int screenWidthDp = getContainerWidth();
        final int cardMargin = 3, recyclerMargin = 10, minimumCardWidth = 100, maximumCardWidth = 250;

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
//        Log.wtf(TAG, "Window width: " + screenWidthDp + " CardWidth: " + widthCard + " Numcols: " + cardViewColumns );
    }

    public void displayRecordingStatus(){
        if(contact.isPrivateNumber()) {
            recordingStatusView.setVisibility(View.INVISIBLE);
            return;
        }
        if(contact.shouldRecord()) {
            recordingStatusView.setText(R.string.rec_status_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.green));
        }
        else {
            recordingStatusView.setText(R.string.rec_status_not_recording);
            recordingStatusView.setTextColor(getResources().getColor(R.color.red));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK && requestCode == ContactDetailPresenter.EDIT_REQUEST_CODE) {
            presenter.onEditActivityResult(intent.getExtras());
        }
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
            itemView.getLayoutParams().width = AppLibrary.pxFromDp(parentActivity, widthCard);
            itemView.getLayoutParams().height = AppLibrary.pxFromDp(parentActivity, widthCard);

            //dimensiunile simbolului pentru sunet:
            int soundSymbolWidth = (int) Math.floor(widthCard * soundSymbolToCardRatio);
            int soundSymbolHeight = (int) (soundSymbolHeightToWidthRatio * soundSymbolWidth);
            soundSymbol.getLayoutParams().width = AppLibrary.pxFromDp(parentActivity,soundSymbolWidth);
            soundSymbol.getLayoutParams().height = AppLibrary.pxFromDp(parentActivity,soundSymbolHeight);

            //marginile TextView-urilor cu data și ora:
            RelativeLayout.LayoutParams lpRecordingDate = (RelativeLayout.LayoutParams) recordingDate.getLayoutParams();
            lpRecordingDate.setMargins(0,
                    AppLibrary.pxFromDp(parentActivity, (int )(widthCard * dateAndTimeMarginsToCardRatio)), 0, 0);
            recordingDate.setLayoutParams(lpRecordingDate);
            RelativeLayout.LayoutParams lpRecordingTime = (RelativeLayout.LayoutParams) recordingTime.getLayoutParams();
            lpRecordingTime.setMargins(0,
                    0, 0, AppLibrary.pxFromDp(parentActivity, (int )(widthCard * dateAndTimeMarginsToCardRatio)));
            recordingTime.setLayoutParams(lpRecordingTime);

            //dimensiunile și marginile recordingType:
            RelativeLayout.LayoutParams lpRecordingType = (RelativeLayout.LayoutParams) recordingType.getLayoutParams();
            lpRecordingType.width = AppLibrary.pxFromDp(parentActivity, (int) (widthCard * recordingTypeToCardRatio));
            lpRecordingType.height = AppLibrary.pxFromDp(parentActivity, (int) (widthCard * recordingTypeToCardRatio));
            lpRecordingType.setMargins(
                    AppLibrary.pxFromDp(parentActivity, (int)(widthCard * recordingTypeMarginsToCardRatio)), 0, 0,
                    AppLibrary.pxFromDp(parentActivity, (int)(widthCard * recordingTypeMarginsToCardRatio)) );
            recordingType.setLayoutParams(lpRecordingType);

            //dimensiunile și marginile tickului selected:
            RelativeLayout.LayoutParams lpRecordingSelected = (RelativeLayout.LayoutParams) recordingSelected.getLayoutParams();
            lpRecordingSelected.width = AppLibrary.pxFromDp(parentActivity, (int) (widthCard * selectedTickToCardRatio));
            lpRecordingSelected.height = AppLibrary.pxFromDp(parentActivity, (int) (widthCard * selectedTickToCardRatio));
            lpRecordingSelected.setMargins(0,
                    AppLibrary.pxFromDp(parentActivity, (int )(widthCard * selectedTickMarginsToCardRatio)),
                    AppLibrary.pxFromDp(parentActivity, (int)(widthCard * selectedTickMarginsToCardRatio)), 0 );
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
            presenter.selectRecording((CardView) v, this.getAdapterPosition());
            return true;
        }

        @Override
        public void onClick(View v) {
            if(getSelectMode()) {
                presenter.selectRecording((CardView) v, this.getAdapterPosition());
            }
            else { //usual short click
                presenter.startPlayerActivity(((RecordingAdapter) recordingsRecycler.getAdapter()).
                        getItem(getAdapterPosition()));
            }
        }
    }

    class RecordingAdapter extends RecyclerView.Adapter<RecordingHolder> {
        List<Recording> recordings;

        void replaceData(List<Recording> recordings) {
            this.recordings = recordings;
            notifyDataSetChanged();
        }

        RecordingAdapter(List<Recording> recordings) {
            this.recordings = recordings;
        }

        @Override
        @NonNull
        public RecordingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parentActivity);
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
            if(selectedItems.contains(position))
                toggleSelectedRecording((CardView) holder.itemView);
        }

        @Override
        public int getItemCount() {
            return recordings.size();
        }

    }
}
