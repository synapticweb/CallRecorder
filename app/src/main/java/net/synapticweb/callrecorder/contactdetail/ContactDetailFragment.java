/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.BaseActivity.LayoutType;
import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.contactdetail.di.ViewModule;
import net.synapticweb.callrecorder.contactslist.ContactsListFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.player.PlayerActivity;
import net.synapticweb.callrecorder.recorder.Recorder;
import net.synapticweb.callrecorder.contactdetail.ContactDetailContract.Presenter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static net.synapticweb.callrecorder.contactslist.ContactsListFragment.ARG_CONTACT;
import net.synapticweb.callrecorder.Util.DialogInfo;

import javax.inject.Inject;

public class ContactDetailFragment extends Fragment implements ContactDetailContract.View {
    @Inject
    Presenter presenter;
    protected RecordingAdapter adapter;
    private TextView typePhoneView, phoneNumberView, recordingStatusView;
    private ImageView contactPhotoView;
    protected RecyclerView recordingsRecycler;
    private RelativeLayout detailView;
    protected Contact contact;
    protected boolean selectMode = false;
    protected List<Integer> selectedItems = new ArrayList<>();
    protected BaseActivity parentActivity;
    /** Dacă există cel puțin un recording lipsă pe disc printre cele selectate, butonul de move se dezactivează.
     *  Cind sunt 0 recorduri lispă se reactivează.*/
    private int selectedItemsDeleted = 0;
    /** Un flag care este setat cînd un recording este șters. Semnifică faptul că fragmentul detaliu curent
     * nu mai este valabil și trebuie înlocuit.TODO: de testat ce se întîmplă cînd se adaugă recording. */
    private boolean invalid = false;
    private static final String SELECT_MODE_KEY = "select_mode_key";
    private static final String SELECTED_ITEMS_KEY = "selected_items_key";
    protected static final int REQUEST_PICK_NUMBER = 2;
    private static final int EFFECT_TIME = 250;
    static final String EDIT_EXTRA_CONTACT = "edit_extra_contact";
    private static final int REQUEST_EDIT = 1;
    public static final String RECORDING_EXTRA = "recording_extra";

    @Nullable
    @Override
    public Context getContext() {
        return super.getContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new RecordingAdapter(new ArrayList<>(0));
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        detailView = (RelativeLayout) inflater.inflate(R.layout.contact_detail_fragment, container, false);
        typePhoneView = detailView.findViewById(R.id.phone_type_detail);
        phoneNumberView = detailView.findViewById(R.id.phone_number_detail);
        contactPhotoView = detailView.findViewById(R.id.contact_photo_detail);
        recordingsRecycler = detailView.findViewById(R.id.recordings);
        //workaround necesar pentru că, dacă recyclerul cu recordinguri conține imagini poza asta devine neagră.
        // Se pare că numai pe lolipop, de verificat. https://github.com/hdodenhof/CircleImageView/issues/31
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            contactPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        calculateCardViewDimensions();
        recordingsRecycler.setLayoutManager(new LinearLayoutManager(parentActivity));
        recordingsRecycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));
        recordingsRecycler.setAdapter(adapter);

        return detailView;
    }

    @Override
    public void onResume(){
        super.onResume();
        presenter.loadRecordings(contact);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        parentActivity = (BaseActivity) context;
        CrApp application = (CrApp) parentActivity.getApplication();
        ViewModule viewModule = new ViewModule(this);
        application.appComponent.contactDetailComponent().create(viewModule).inject(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parentActivity = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_EDIT) {
                Bundle result = intent.getExtras();
                if(result != null) {
                    setContact(result.getParcelable(EditContactActivity.EDITED_CONTACT));
                    if(parentActivity.getLayoutType() == LayoutType.SINGLE_PANE) {
                        TextView title = parentActivity.findViewById(R.id.actionbar_title);
                        title.setText(contact.getContactName());
                    }
                }
            }
            else if (requestCode == REQUEST_PICK_NUMBER) {
                Uri numberUri = intent.getData();
                if (numberUri != null)
                    onAssignToContact(numberUri);
            }
        }
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

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    /**
     * Funcție apelată de presenter după ce termină de încărcat recordingurile. Afișează toate elementele
     * fragmentului ținînd cont de valoarea selectMode. Recordingurile sunt puse în starea corespunzătoare
     * nu aici, ci în onBindViewHolder.
     * @param recordings O listă cu recordinguri extrase de presenter și oferite fragmentului spre a fi afișate.
     */
    @Override
    public void paintViews(List<Recording> recordings){
        adapter.replaceData(recordings);
        if(selectMode)
            putInSelectMode(false);
            //necesar pentru că 1: dacă în DOUBLE_PANE se clichează pe un contact în timp ce sunt selectate
            //recordinguri, actionbar-ul rămîne în select mode. 2: rezolvă un alt bug: dacă există un
            //contact hidden și se clickează pe el, apoi pe unul normal butoanele call și edit rămîn ascunse.
        else
            toggleSelectModeActionBar(false);
        typePhoneView.setText(Util.getSpannedText(String.format(getResources().getString(
                R.string.detail_phonetype), contact.getPhoneTypeName()), null));
        phoneNumberView.setText(Util.getSpannedText(String.format(getResources().getString(
                R.string.detail_phonenumber), contact.isPrivateNumber() ? getString(R.string.private_number_name) : contact.getPhoneNumber()), null));

        if(contact.getPhotoUri() != null) {
            contactPhotoView.clearColorFilter();
            contactPhotoView.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
            contactPhotoView.setImageURI(contact.getPhotoUri());
        }
        else {
            if(contact.isPrivateNumber())
                contactPhotoView.setImageResource(R.drawable.incognito);
            else {
                contactPhotoView.setImageResource(R.drawable.user_contact);
                contactPhotoView.setColorFilter(new
                        PorterDuffColorFilter(contact.getColor(), PorterDuff.Mode.LIGHTEN));
            }
        }

        TextView noContent = detailView.findViewById(R.id.no_content_detail);
        if(recordings.size() > 0)
            noContent.setVisibility(View.GONE);
        else
            noContent.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean isInvalid() { return invalid; }
    @Override
    public void setInvalid(boolean invalid) { this.invalid = invalid; }

    /**
     * Această funcție este inclusă în interfața View. Rolul ei este să șteargă recordinguri din adapter cînd
     * sunt șterse sau asignate altui contact de către presenter. În mod normal presenterul nu ar trebui să se
     * amestece în treburile unui fragment, dar dacă las updatarea adapterului pe seama fragmentului (după ce
     * rulează codul din presenter), e posibil să apară neconcordanțe cu realitatea în cazul în care are loc o
     * excepție în presenter.
     * @param recording recordingul care trebuie șters.
     */
    @Override
    public void removeRecording(Recording recording) {
        adapter.removeItem(recording);
    }

    /**
     * Introduce fragmentul în modul selectOn.
     * @param animate Dacă transformarea actionBarului se va face cu animație sau fără.
     */
    protected void putInSelectMode(boolean animate) {
        selectMode = true;
        toggleSelectModeActionBar(animate);
        redrawRecordings();
    }

    /**
     * Transformă aparența actionBarului în funcție de flagul selectMode.
     * @param animate Dacă afișările și disparițiile vor fi animate sau bruște. Cînd este apelat în paintView
     * nu e nevoie de animație. Cînd e apelat din onLongClick transformările sunt animate.
     */
    protected void toggleSelectModeActionBar(boolean animate) {
        ImageButton navigateBackBtn = parentActivity.findViewById(R.id.navigate_back);
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        ImageButton editBtn = parentActivity.findViewById(R.id.edit_contact);
        ImageButton callBtn = parentActivity.findViewById(R.id.call_contact);
        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        ImageButton menuRightBtn = parentActivity.findViewById(R.id.contact_detail_menu);
        ImageButton menuRightSelectedBtn = parentActivity.findViewById(R.id.contact_detail_selected_menu);

        toggleTitle();
        if(parentActivity.getLayoutType() == LayoutType.SINGLE_PANE)
            if(selectMode) hideView(navigateBackBtn, animate); else showView(navigateBackBtn, animate);

        if(selectMode) showView(closeBtn, animate); else hideView(closeBtn, animate);
        if(!contact.isPrivateNumber()) {
            if(selectMode) hideView(editBtn, animate); else showView(editBtn, animate);
            if(selectMode) hideView(callBtn, animate); else showView(callBtn, animate);
        }
        if(selectMode) showView(moveBtn, animate); else hideView(moveBtn, animate);
        if(selectMode) {
            if(checkIfSelectedRecordingsDeleted())
                disableMoveBtn();
            else
                enableMoveBtn();
        }
        if(selectMode) showView(selectAllBtn, animate); else  hideView(selectAllBtn, animate);
        if(selectMode) showView(infoBtn, animate); else hideView(infoBtn, animate);
        if(selectMode) showView(menuRightSelectedBtn, animate); else hideView(menuRightSelectedBtn, animate);
        if(selectMode) hideView(menuRightBtn, animate); else showView(menuRightBtn, animate);

        if(parentActivity.getLayoutType() == LayoutType.DOUBLE_PANE) {
            ImageButton hamburger = parentActivity.findViewById(R.id.hamburger);
            if(selectMode) hideView(hamburger, animate); else showView(hamburger, animate);
        }
    }

    /**
     * Modifică titlul și poziția actionBarului în funcție de selectMode și tipul de layout.
     */
    protected void toggleTitle() {
        TextView title = parentActivity.findViewById(R.id.actionbar_title);
        if(parentActivity.getLayoutType() == LayoutType.DOUBLE_PANE) {
            Toolbar.LayoutParams params = (Toolbar.LayoutParams) title.getLayoutParams();
            params.gravity = selectMode ? Gravity.START : Gravity.CENTER;
            title.setLayoutParams(params);
        }

        if(selectMode)
            title.setText(String.valueOf(selectedItems.size()));
        else {
            if(parentActivity.getLayoutType() == LayoutType.SINGLE_PANE)
                title.setText(contact.getContactName());
            else
                title.setText(R.string.app_name);
        }
    }

    /**
     * Afișează sau ascunde un element cu un efect de fade-in sau fade-out care durează EFFECT_TIME
     * @param view Elementul care va fi afișat sau ascuns
     * @param finalAlpha valoarea alpha la care se ajunge în final
     * @param finalVisibility vi***zibilitatea finală: View.VISIBLE sau View.GONE
     */
    private void fadeEffect(View view, float finalAlpha, int finalVisibility) {
        view.animate()
                .alpha(finalAlpha)
                .setDuration(EFFECT_TIME)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                    }
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        view.setVisibility(finalVisibility);
                    }
                    @Override
                    public void onAnimationCancel(Animator animator) {
                    }
                    @Override
                    public void onAnimationRepeat(Animator animator) {
                    }
                });
    }

    protected void hideView(View view, boolean animate) {
        if(animate)
            fadeEffect(view, 0.0f, View.GONE);
        else {
            view.setAlpha(0.0f); //poate lipsi?
            view.setVisibility(View.GONE);
        }
    }

    protected void showView(View view, boolean animate) {
        if(animate)
            fadeEffect(view, 1f, View.VISIBLE);
        else {
            view.setAlpha(1f); //poate lipsi?
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Scoate fragmentul din modul selectOn.Transformarea actionBarului va fi întotdeauna animată.
     */
    protected void clearSelectMode() {
        selectMode = false;
        toggleSelectModeActionBar(true);
        redrawRecordings();
        selectedItems.clear();
    }

    /**
     * Modifică în funcție de selectMode marginile elementelor care compun vizual un recording. De asemenea
     * afișează sau ascunde căsuța de check.
     * @param recording Recordingul care va fi transformat.
     */
    private void modifyMargins(View recording) {
        CheckBox checkBox = recording.findViewById(R.id.recording_checkbox);
        Resources res = getContext().getResources();
        checkBox.setVisibility((selectMode ? View.VISIBLE : View.GONE));
        RelativeLayout.LayoutParams lpCheckBox = (RelativeLayout.LayoutParams) checkBox.getLayoutParams();
        lpCheckBox.setMarginStart(selectMode ?
                (int) res.getDimension(R.dimen.recording_checkbox_visible_start_margin) :
                (int) res.getDimension(R.dimen.recording_checkbox_gone_start_margin));
        checkBox.setLayoutParams(lpCheckBox);

        ImageView recordingAdorn = recording.findViewById(R.id.recording_adorn);
        RelativeLayout.LayoutParams lpRecAdorn = (RelativeLayout.LayoutParams) recordingAdorn.getLayoutParams();
        lpRecAdorn.setMarginStart(selectMode ?
                (int) res.getDimension(R.dimen.recording_adorn_selected_margin_start) :
                (int) res.getDimension(R.dimen.recording_adorn_unselected_margin_start));
        recordingAdorn.setLayoutParams(lpRecAdorn);

        TextView title = recording.findViewById(R.id.recording_title);
        RelativeLayout.LayoutParams lpTitle = (RelativeLayout.LayoutParams) title.getLayoutParams();
        lpTitle.setMarginStart(selectMode ?
                (int) res.getDimension(R.dimen.recording_title_selected_margin_start) :
                (int) res.getDimension(R.dimen.recording_title_unselected_margin_start));
        title.setLayoutParams(lpTitle);
    }

    private void selectRecording(@NonNull android.view.View recording) {
        CheckBox checkBox = recording.findViewById(R.id.recording_checkbox);
        checkBox.setChecked(true);
    }

    private void deselectRecording(View recording) {
        CheckBox checkBox = recording.findViewById(R.id.recording_checkbox);
        checkBox.setChecked(false);
    }

    protected void enableMoveBtn() {
        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        moveBtn.setEnabled(true);
        moveBtn.setImageAlpha(255);
    }

    protected void disableMoveBtn() {
        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        moveBtn.setEnabled(false);
        moveBtn.setImageAlpha(75);
    }

    /**
     * Apelată cînd se intră sau se iese din selectMode=on. Funcția nu face decît să notifice adapterul că
     * recordingurile s-au schimbat. După notificare ele sunt reafișate și este apelată onBindViewHolder care
     * va apela modifyMargins(). Desigur, notifyDataSetChanged() ar fi fost mai simplu. Dar dacă fac așa
     * tranziția este prea bruscă.
     */
    private void redrawRecordings() {
        for(int i = 0; i < adapter.getItemCount(); ++i)
            adapter.notifyItemChanged(i);
    }

    /**
     * Apelată cînd se clichează lung (selectMode=false) sau scurt(selectMode=true) pe un recording. Adaugă
     * poziția din adapter a recordingului în lista selectedItems și selectează ori deselectează itemul. Cînd
     * se deselectează ultimul item se iese din selectMode=true.
     * @param recording recordingul clicat
     * @param adapterPosition poziția lui în adapter
     * @param exists dacă există pe disc
     */
    private void manageSelectRecording(View recording, int adapterPosition, boolean exists) {
        if(!removeIfPresentInSelectedItems(adapterPosition)) {
            selectedItems.add(adapterPosition);
            selectRecording(recording);
            if(!exists) {
                selectedItemsDeleted++;
                disableMoveBtn();
            }
        }
        else {
            deselectRecording(recording);
            if(!exists)
                selectedItemsDeleted--;
            if(selectedItemsDeleted == 0)
                enableMoveBtn();
        }

        if(selectedItems.isEmpty())
            clearSelectMode();
        else
            toggleTitle();
    }


    /**
     * Transformă colecția de poziții în adapter selectedItems într-o colecție de Recordinguri
     * @return O listă de obiecte Recording
     */
    private List<Recording> getSelectedRecordings() {
        List<Recording> list = new ArrayList<>();
        for(int adapterPosition : selectedItems)
            list.add(adapter.getItem(adapterPosition));
        return list;
    }

    /**
     * Apelată de {@code manageSelectRecording()} cu poziția în adapter a recordingului pe care s-a clicat
     * (obținută cu adapter.getAdapterPosition()). Dacă poziția respectivă există deja în selectedItems o
     * scoate de acolo (deselectează).
     * @param adapterPosition o poziție în selectedItems
     * @return Dacă poziția există în selectedItems întoarce {@code true}. Altfel întoarce {@code false}.
     */
    private boolean removeIfPresentInSelectedItems(int adapterPosition) {
        if (selectedItems.contains(adapterPosition)) {
            selectedItems.remove((Integer) adapterPosition); //fără casting îl interpretează ca poziție
            //în selectedItems
            return true;
        }
        else
            return false;
    }

    /**
     * Apelată de toggleSelectModeActionBar pentru a afla dacă există vreun recording selectat care este
     * lipsă pe disc. În caz că da va dezactiva mutarea recordingurilor.
     */
    protected boolean checkIfSelectedRecordingsDeleted() {
        for(Recording recording : getSelectedRecordings())
            if(!recording.exists())
                return true;
            return false;
    }


    public static ContactDetailFragment newInstance(Contact contact) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONTACT, contact);
        ContactDetailFragment fragment = new ContactDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void onDeleteContact() {
        new MaterialDialog.Builder(parentActivity)
                .title(R.string.delete_contact_confirm_title)
                .content(String.format(getResources().
                        getString(R.string.delete_contact_confirm_message), contact.getContactName()))
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .icon(getResources().getDrawable(R.drawable.warning))
                .onPositive((@NonNull MaterialDialog dialog, @NonNull DialogAction which) -> {
                        DialogInfo result = presenter.deleteContact(contact);
                        if(result != null) {
                            new MaterialDialog.Builder(parentActivity)
                                    .title(result.title)
                                    .content(result.message)
                                    .iconRes(result.icon)
                                    .positiveText(android.R.string.ok)
                                    .show();
                            return ;
                        }

                    if(parentActivity.getLayoutType() == LayoutType.DOUBLE_PANE) {
                        setInvalid(true);
                        ContactsListFragment listFragment = (ContactsListFragment)
                                parentActivity.getSupportFragmentManager().findFragmentById(R.id.contacts_list_fragment_container);
                        if(listFragment != null) {
                            listFragment.resetCurrentPosition();
                            listFragment.onResume();
                        }
                    }
                    else
                        parentActivity.finish();
                    }
                )
                .show();
    }

    /**
     * Acțiunile care trebuie realizate în interfață cînd un recording este asignat la un nr normal
     * sau privat sunt identice. Motiv pentru care apelez de 2 ori această metodă.
     * @param result Rezultatul apelării metodei presenterului.
     */
    private void onAssignViewActions(DialogInfo result) {
        if(result.icon == R.drawable.success) {
            if(adapter.getItemCount() == 0) {
                View noContent = parentActivity.findViewById(R.id.no_content_detail);
                if(noContent != null)
                    noContent.setVisibility(View.VISIBLE);
            }

            if(parentActivity.getLayoutType() == LayoutType.DOUBLE_PANE) {
                ContactsListFragment listFragment = (ContactsListFragment)
                        parentActivity.getSupportFragmentManager().findFragmentById(R.id.contacts_list_fragment_container);
                setInvalid(true); //e nevoie?
                if (listFragment != null) {
                    listFragment.onResume();
                }
            }
            clearSelectMode();
        }
        new MaterialDialog.Builder(parentActivity)
                .title(result.title)
                .content(result.message)
                .positiveText(android.R.string.ok)
                .icon(getResources().getDrawable(result.icon))
                .show();
    }

    private void onAssignToContact(Uri numberUri) {
        List<Recording> recordings = getSelectedRecordings();
        DialogInfo result = presenter.assignToContact(parentActivity, numberUri, recordings, contact);
        onAssignViewActions(result);
    }

    protected void onAssignToPrivate() {
        DialogInfo result = presenter.assignToPrivate(parentActivity, getSelectedRecordings(), contact);
        onAssignViewActions(result);
    }

    private void onShowStorageInfo() {
        long sizePrivate = 0, sizePublic = 0;
        for(Recording recording : adapter.getRecordings()) {
            long size = new File(recording.getPath()).length();
            if(recording.isSavedInPrivateSpace(parentActivity))
                sizePrivate += size;
            else
                sizePublic += size;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(parentActivity)
                .title(R.string.storage_info)
                .customView(R.layout.info_storage_dialog, false)
                .positiveText(android.R.string.ok).build();
        TextView privateStorage = dialog.getView().findViewById(R.id.info_storage_private_data);
        privateStorage.setText(Util.getFileSizeHuman(sizePrivate));

        TextView publicStorage = dialog.getView().findViewById(R.id.info_storage_public_data);
        publicStorage.setText(Util.getFileSizeHuman(sizePublic));

        dialog.show();
    }

    protected void onRenameRecording() {
        new MaterialDialog.Builder(parentActivity)
                .title(R.string.rename_recording_title)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(parentActivity.getResources().getString(R.string.rename_recording_input_text),
                        null, false, (@NonNull MaterialDialog dialog, CharSequence input) -> {
                            if(selectedItems.size() != 1) {
                                CrLog.log(CrLog.WARN, "Calling onRenameClick when multiple recordings are selected");
                                return ;
                            }
                            DialogInfo result = presenter.renameRecording(input, getSelectedRecordings().get(0));
                            if(result != null)
                                new MaterialDialog.Builder(parentActivity)
                                        .title(result.title)
                                .content(result.message)
                                .icon(getResources().getDrawable(result.icon))
                                .positiveText(android.R.string.ok)
                                .show();
                            else
                                adapter.notifyItemChanged(selectedItems.get(0));
                        }
                ).show();
    }

    protected void onDeleteSelectedRecordings() {
        new MaterialDialog.Builder(parentActivity)
                .title(R.string.delete_recording_confirm_title)
                .content(String.format(getResources().getString(
                        R.string.delete_recording_confirm_message),
                        selectedItems.size()))
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .icon(parentActivity.getResources().getDrawable(R.drawable.warning))
                .onPositive((@NonNull MaterialDialog dialog,
                             @NonNull DialogAction which) -> {

                    DialogInfo result = presenter.deleteRecordings(getSelectedRecordings());
                    if(result != null)
                        new MaterialDialog.Builder(parentActivity)
                                .title(result.title)
                                .content(result.message)
                                .icon(getResources().getDrawable(result.icon))
                                .positiveText(android.R.string.ok)
                                .show();
                    else {
                        if(adapter.getItemCount() == 0) {
                            View noContent  = parentActivity.findViewById(R.id.no_content_detail);
                            if(noContent != null)
                                noContent.setVisibility(View.VISIBLE);
                        }
                        clearSelectMode();
                    }
                })
                .show();
    }

    protected void onSelectAll() {
        List<Integer> notSelected = new ArrayList<>();
        for(int i = 0; i < adapter.getItemCount(); ++i)
            notSelected.add(i);
        notSelected.removeAll(selectedItems);

        for(int position : notSelected) {
            selectedItems.add(position);
            adapter.notifyItemChanged(position);
            //https://stackoverflow.com/questions/33784369/recyclerview-get-view-at-particular-position
            View selectedRecording = recordingsRecycler.getLayoutManager().findViewByPosition(position);
            if(selectedRecording != null) //dacă recordingul nu este încă afișat pe ecran
            // (sunt multe recordinguri și se scrolează) atunci selectedRecording va fi null. Dar mai înainte am
            //notificat adapterul că s-a schimbat, ca să îl reconstruiască.
                 selectRecording(selectedRecording);
            }
        toggleTitle();
    }

    protected void onRecordingInfo() {
        if (selectedItems.size() > 1) {
            long totalSize = 0;
            for (int position : selectedItems) {
                Recording recording = adapter.getItem(position);
                totalSize += recording.getSize();
            }
            new MaterialDialog.Builder(parentActivity)
                    .title(R.string.recordings_info_title)
                    .content(String.format(parentActivity.getResources().getString(R.string.recordings_info_text), Util.getFileSizeHuman(totalSize)))
                    .positiveText(android.R.string.ok)
                    .show();
            return ;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(parentActivity)
                .title(R.string.recording_info_title)
                .customView(R.layout.info_dialog, false)
                .positiveText(android.R.string.ok).build();

        //There should be only one if we are here:
        if(selectedItems.size() != 1) {
            CrLog.log(CrLog.WARN, "Calling onInfoClick when multiple recordings are selected");
            return ;
        }

        Recording recording = adapter.getItem(selectedItems.get(0));
        TextView date = dialog.getView().findViewById(R.id.info_date_data);
        date.setText(String.format("%s %s", recording.getDate(), recording.getTime()));
        TextView size = dialog.getView().findViewById(R.id.info_size_data);
        size.setText(Util.getFileSizeHuman(recording.getSize()));
        TextView source = dialog.getView().findViewById(R.id.info_source_data);
        source.setText(recording.getSource());

        TextView format = dialog.getView().findViewById(R.id.info_format_data);
        format.setText(recording.getHumanReadingFormat(parentActivity));
        TextView length = dialog.getView().findViewById(R.id.info_length_data);
        length.setText(Util.getDurationHuman(recording.getLength(), true));
        TextView path = dialog.getView().findViewById(R.id.info_path_data);
        path.setText(recording.isSavedInPrivateSpace(parentActivity) ? parentActivity.getResources().
                getString(R.string.private_storage) : recording.getPath());
        if(!recording.exists()) {
            path.setText(String.format("%s%s", path.getText(), parentActivity.getResources().
                    getString(R.string.nonexistent_file)));
            path.setTextColor(parentActivity.getResources().getColor(android.R.color.holo_red_light));
        }
        dialog.show();
    }

    private void onMoveSelectedRecordings(String path) {
        int totalSize = 0;
        List<Recording> recordings = getSelectedRecordings();
        Recording[] recordingsArray = new Recording[recordings.size()];

        for(Recording recording : recordings) {
            if(new File(recording.getPath()).getParent().equals(path)) {
                new MaterialDialog.Builder(parentActivity)
                        .title(R.string.information_title)
                        .content(R.string.move_destination_same)
                        .positiveText("OK")
                        .icon(getResources().getDrawable(R.drawable.info))
                        .show();
                return ;
            }
            totalSize += new File(recording.getPath()).length();
        }

        presenter.moveSelectedRecordings(path, totalSize, parentActivity, recordings.toArray(recordingsArray));
    }

    protected void setDetailsButtonsListeners() {
        ImageButton navigateBack = parentActivity.findViewById(R.id.navigate_back);
        navigateBack.setOnClickListener((view) ->
                NavUtils.navigateUpFromSameTask(parentActivity)
        );
        final ImageButton menuButtonSelectOff = parentActivity.findViewById(R.id.contact_detail_menu);
        menuButtonSelectOff.setOnClickListener((view) -> {
//pentru micșorarea fontului se folosește constructorul PopupMenu(ContextThemeWrapper, v). E necesar un wrapper
// în jurul unui stil din styles.xml. Stilul trebuie să moștenească din Theme.AppCompat.Light.NoActionBar
// pentru temele light și din Theme.AppCompat.NoActionBar pentru cele dark, altfel background-ul
// va avea culoarea greșită.
                PopupMenu popupMenu = new PopupMenu(parentActivity,view);
                popupMenu.setOnMenuItemClickListener((item) -> {
                        switch (item.getItemId()) {
                            case R.id.delete_phone_number:
                                onDeleteContact();
                                return true;
                            case R.id.storage_info:
                                onShowStorageInfo();
                            default:
                                return false;
                        }
                });
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.contact_popup, popupMenu.getMenu());
                popupMenu.show();
            });

        ImageButton editContact = parentActivity.findViewById(R.id.edit_contact);
        ImageButton callContact = parentActivity.findViewById(R.id.call_contact);
        if(contact.isPrivateNumber()) {
            callContact.setVisibility(View.GONE);
            editContact.setVisibility(View.GONE);
        }
        else {
            callContact.setOnClickListener((View v) -> {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.getPhoneNumber(), null));
                startActivity(intent);
            });

            editContact.setOnClickListener((View v) -> {
                Intent intent = new Intent(parentActivity, EditContactActivity.class);
                intent.putExtra(EDIT_EXTRA_CONTACT, contact);
                startActivityForResult(intent, REQUEST_EDIT);
                }
            );

        }
        ImageButton closeBtn = parentActivity.findViewById(R.id.close_select_mode);
        closeBtn.setOnClickListener((View v) -> clearSelectMode() );

        final ImageButton menuButtonSelectOn = parentActivity.findViewById(R.id.contact_detail_selected_menu);
        menuButtonSelectOn.setOnClickListener((View view) -> {
                PopupMenu popupMenu = new PopupMenu(parentActivity, view);
                popupMenu.setOnMenuItemClickListener((MenuItem item) -> {
                        switch (item.getItemId()) {
                            case R.id.rename_recording:
                                onRenameRecording();
                                return true;
                            case R.id.delete_recording:
                                onDeleteSelectedRecordings();
                                return true;
                            case R.id.assign_to_contact:
                                Intent pickNumber = new Intent(Intent.ACTION_PICK,
                                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                                startActivityForResult(pickNumber, REQUEST_PICK_NUMBER);
                                return true;
                            case R.id.assign_private:
                                onAssignToPrivate();
                                return true;
                            default:
                                return false;
                        }
                    });

                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.recording_selected_popup, popupMenu.getMenu());
                MenuItem renameMenuItem = popupMenu.getMenu().findItem(R.id.rename_recording);
                Recording recording = ((RecordingAdapter) recordingsRecycler.getAdapter()).
                        getItem(selectedItems.get(0));
                if(selectedItems.size() > 1 || !recording.exists())
                    renameMenuItem.setEnabled(false);
                popupMenu.show();
            }
        );

        ImageButton moveBtn = parentActivity.findViewById(R.id.actionbar_select_move);
        registerForContextMenu(moveBtn);
        //foarte necesar. Altfel meniul contextual va fi arătat numai la long click.
        moveBtn.setOnClickListener(View::showContextMenu);

        ImageButton selectAllBtn = parentActivity.findViewById(R.id.actionbar_select_all);
        selectAllBtn.setOnClickListener((View v) -> onSelectAll());

        ImageButton infoBtn = parentActivity.findViewById(R.id.actionbar_info);
        infoBtn.setOnClickListener((View view) -> onRecordingInfo());
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = parentActivity.getMenuInflater();
        inflater.inflate(R.menu.storage_chooser_options, menu);

        boolean allowMovePrivate = true;
        for(Recording recording : getSelectedRecordings())
            if(recording.isSavedInPrivateSpace(parentActivity)) {
                allowMovePrivate = false;
                break;
            }
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spanString.length();
            spanString.setSpan(new RelativeSizeSpan(0.87f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spanString);
        }

        MenuItem menuItem = menu.getItem(0);
        menuItem.setEnabled(allowMovePrivate);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.private_storage: onMoveSelectedRecordings(parentActivity.
                    getFilesDir().getAbsolutePath());
                return true;
            case R.id.public_storage:
                Content content = new Content();
                content.setOverviewHeading(parentActivity.getResources().getString(R.string.move_heading));
                StorageChooser.Theme theme = new StorageChooser.Theme(parentActivity);
                theme.setScheme(parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                        parentActivity.getResources().getIntArray(R.array.storage_chooser_theme_light) :
                        parentActivity.getResources().getIntArray(R.array.storage_chooser_theme_dark));

                StorageChooser chooser = new StorageChooser.Builder()
                        .withActivity(parentActivity)
                        .withFragmentManager(parentActivity.getFragmentManager())
                        .allowCustomPath(true)
                        .setType(StorageChooser.DIRECTORY_CHOOSER)
                        .withMemoryBar(true)
                        .allowAddFolder(true)
                        .showHidden(true)
                        .withContent(content)
                        .setTheme(theme)
                        .build();

                chooser.show();

                chooser.setOnSelectListener(this::onMoveSelectedRecordings);
                return true;

            default: return super.onContextItemSelected(item);
        }
    }

    class RecordingHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView title;
        ImageView recordingType, recordingAdorn, exclamation;
        CheckBox checkBox;

        RecordingHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.recording, parent, false));
            recordingType = itemView.findViewById(R.id.recording_type);
            title = itemView.findViewById(R.id.recording_title);
            checkBox = itemView.findViewById(R.id.recording_checkbox);
            recordingAdorn = itemView.findViewById(R.id.recording_adorn);
            exclamation = itemView.findViewById(R.id.recording_exclamation);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if(!selectMode)
                putInSelectMode(true);
            Recording recording = adapter.getItem(getAdapterPosition());
            manageSelectRecording(v, this.getAdapterPosition(), recording.exists());
            return true;
        }

        @Override
        public void onClick(View v) {
            Recording recording = adapter.getItem(getAdapterPosition());
            if(selectMode)
                manageSelectRecording(v, this.getAdapterPosition(), recording.exists());
            else { //usual short click
                if(recording.exists()) {
                    Intent playIntent = new Intent(parentActivity, PlayerActivity.class);
                    playIntent.putExtra(RECORDING_EXTRA, recording);
                    startActivity(playIntent);
                }
                else
                    Toast.makeText(parentActivity, R.string.audio_file_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //A devenit public pentru a funcționa adapter.replaceData() în UnassignedRecordingsFragment
    public class RecordingAdapter extends RecyclerView.Adapter<RecordingHolder> {
        private List<Recording> recordings;

        List<Recording> getRecordings() {
            return recordings;
        }

        //A devenit public pentru a funcționa adapter.replaceData() în UnassignedRecordingsFragment
        public void replaceData(List<Recording> recordings) {
            this.recordings = recordings;
            notifyDataSetChanged();
        }

        void removeItem(Recording recording) {
            int position = recordings.indexOf(recording);
            recordings.remove(recording);
            notifyItemRemoved(position);
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

        //A devenit public pentru a funcționa UnassignedRecordingsFragment
        public Recording getItem(int position) {
            return recordings.get(position);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordingHolder holder, final int position) {
            final Recording recording = recordings.get(position);
            int adornRes;
            switch (recording.getFormat()) {
                case Recorder.WAV_FORMAT: adornRes = parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                        R.drawable.sound_symbol_wav_light : R.drawable.sound_symbol_wav_dark;
                    break;
                case Recorder.AAC_HIGH_FORMAT: adornRes = parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                        R.drawable.sound_symbol_aac128_light : R.drawable.sound_symbol_aac128_dark;
                    break;
                case Recorder.AAC_BASIC_FORMAT: adornRes = parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                        R.drawable.sound_symbol_aac32_light : R.drawable.sound_symbol_aac32_dark;
                    break;
                default:adornRes = parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                        R.drawable.sound_symbol_aac64_light : R.drawable.sound_symbol_aac64_dark;
            }

            holder.title.setText(recording.getName());
            if(contact == null || !contact.isPrivateNumber())
                holder.recordingType.setImageResource(recording.isIncoming() ? R.drawable.incoming :
                    parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                            R.drawable.outgoing_light : R.drawable.outgoing_dark);
            holder.recordingAdorn.setImageResource(adornRes);
            holder.checkBox.setOnClickListener((View view) ->
                    manageSelectRecording(view, position, recording.exists()));

            if(!recording.exists())
                markNonexistent(holder);

            modifyMargins(holder.itemView);
            if(selectedItems.contains(position))
                selectRecording(holder.itemView);
            else
                deselectRecording(holder.itemView);
        }

        private void markNonexistent(RecordingHolder holder) {
            holder.exclamation.setVisibility(View.VISIBLE);
//https://stackoverflow.com/questions/25454316/how-do-i-partially-gray-out-an-image-when-pressed
            //A se vedea și https://stackoverflow.com/questions/28308325/androidset-gray-scale-filter-to-imageview
            int filter = parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME) ?
                    Color.argb(255,0,0,0) : Color.argb(255,255, 255, 255);
            holder.recordingAdorn.setColorFilter(filter);
            holder.recordingType.setColorFilter(filter);
            holder.recordingAdorn.setImageAlpha(100);
            holder.recordingType.setImageAlpha(100);
            holder.title.setAlpha(0.5f);
        }

        private void unMarkNonexistent(RecordingHolder holder) {
            holder.exclamation.setVisibility(View.GONE);
            holder.recordingAdorn.setColorFilter(null);
            holder.recordingType.setColorFilter(null);
            holder.recordingType.setImageAlpha(255);
            holder.recordingAdorn.setImageAlpha(255);
            holder.title.setAlpha(1f);
        }

        //necesar pentru că, altfel, după ce se intră în select mode, la următoarea randare a listei apar
        //recordinguri cu semnul exclamării cu toate că fișierele există - e din cauză că itemii sunt reciclați.
        @Override
        public void onViewRecycled(@NonNull RecordingHolder holder) {
            super.onViewRecycled(holder);
            unMarkNonexistent(holder);
        }

        @Override
        public int getItemCount() {
            return recordings.size();
        }

    }
}