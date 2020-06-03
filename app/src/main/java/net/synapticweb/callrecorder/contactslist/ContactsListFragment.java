/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.BaseActivity.LayoutType;
import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.contactdetail.ContactDetailActivity;
import net.synapticweb.callrecorder.contactdetail.ContactDetailContract;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.contactslist.di.ViewModule;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListContract.Presenter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import javax.inject.Inject;

public class ContactsListFragment extends Fragment implements ContactsListContract.View {
    @Inject
    Presenter presenter;
    private ContactsAdapter adapter;
    private RecyclerView contactsRecycler;
    /** Poziția curentă în adapter. Necesară doar în DOUBLE_PANE. Este setată din
     * ViewHolder.getAdapterPosition() cînd se clichează și e folosită la setarea detaliului curent și la
     * selectarea contactului curent în onBindViewHolder().*/
    private int currentPos = 0;
    private BaseActivity parentActivity;
    private int colorPointer = 0;
    @SuppressLint("UseSparseArrays")
    private Map<Long, Integer> contactsColors = new HashMap<>();
    private final static String CURRENT_POS_KEY = "current_pos";
    public final static String ARG_CONTACT = "arg_contact";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        parentActivity = (BaseActivity) context;
        CrApp application = (CrApp) parentActivity.getApplication();
        ViewModule viewModule = new ViewModule(this);
        application.appComponent.contactsListComponent().create(viewModule).inject(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        parentActivity = null;
    }

    /** Apelată din ContactDetailFragment după ștergerea unui contact.*/
    @Override
    public void resetCurrentPosition() {
        currentPos = 0;
    }

    @Override
    public void onResume(){
        super.onResume();
        presenter.loadContacts();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_POS_KEY, currentPos);
    }

    /** Se ocupă cu setarea fragmentului detaliu în DOUBLE_PANE. */
    private void manageDetail() {
        //dacă ne găsim după un restart al activității
        // datorat rotirii sau după reluarea activității la venirea din background, nu trebuie să mai înlocuim
        // fragmentul detaliu, el este deja acolo. Dacă îl înlocuim onResume() al fragmnetului detaliu va fi
        // apelat de 2 ori: prima dată din cauza restartului fragmentului detaliu - cu datele de stare,
        // a doua oară datorită înlocuirii - fără datele de stare. Vom face deci înlocuirea fragmentului
        //detaliu doar dacă: a. încă nu s-a încărcat niciun contact b. un contact a fost adăugat c. un contact
        //a fost șters.
        Fragment detailFragment = parentActivity.getSupportFragmentManager().findFragmentById(R.id.contact_detail_fragment_container);
        if(adapter.getItemCount() > 0) {
            if (detailFragment == null || ((ContactDetailContract.View) detailFragment).isInvalid())
                setCurrentDetail(adapter.getItem(currentPos));
        }
        else
            setCurrentDetail(null);
    }

    //e apelată de callback-ul pasat funcției ContactRepository::getContacts(), la rîndul ei apelată de
    // Presenter::loadContacts()
    @Override
    public void showContacts(List<Contact> contacts) {
        adapter = new ContactsAdapter(contacts);
        contactsRecycler.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        if(parentActivity.getLayoutType() == BaseActivity.LayoutType.DOUBLE_PANE)
            manageDetail();

        TextView noContent = parentActivity.findViewById(R.id.no_content_list);
        if(adapter.getItemCount() > 0)
            noContent.setVisibility(View.GONE);
        else
            noContent.setVisibility(View.VISIBLE);
    }

    /** Necesară deoarece există 2 variante de selectContact(). */
    private void realSelectContact(View contactSlot) {
        if(contactSlot != null) {
            contactSlot.findViewById(R.id.tablet_current_selection).setVisibility(View.VISIBLE);
            if(parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME))
                contactSlot.setBackgroundColor(getResources().getColor(R.color.slotLightSelected));
            else
                contactSlot.setBackgroundColor(getResources().getColor(R.color.slotDarkSelected));
        }
    }

    /** Apelată la clicarea pe un contact cu poziția în adapter.  */
    private void selectContact(int position) {
        View contactSlot =  contactsRecycler.getLayoutManager().findViewByPosition(position);
        realSelectContact(contactSlot);
    }

    /** Apelată în onBindViewHolder() cu View-ul corespunzător. */
    private void selectContactWithView(View contactSlot) {
        realSelectContact(contactSlot);
    }

   private void deselectContact(int position) {
        View contactSlot = contactsRecycler.getLayoutManager().findViewByPosition(position);
        if(contactSlot != null) {
            contactSlot.findViewById(R.id.tablet_current_selection).setVisibility(View.GONE);
            if(parentActivity.getSettedTheme().equals(BaseActivity.LIGHT_THEME))
                contactSlot.setBackgroundColor(getResources().getColor(R.color.slotLight));
            else
                contactSlot.setBackgroundColor(getResources().getColor(R.color.slotAndDetailHeaderDark));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            currentPos = savedInstanceState.getInt(CURRENT_POS_KEY);
        }
        this.adapter = new ContactsAdapter(new ArrayList<>(0));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentRoot = inflater.inflate(R.layout.list_contacts_fragment, container, false);
        contactsRecycler = fragmentRoot.findViewById(R.id.listened_phones);
        contactsRecycler.setLayoutManager(new LinearLayoutManager(parentActivity));
        return fragmentRoot;
    }

    public class ContactHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView contactPhoto;
        TextView mContactName;
        TextView mPhoneNumber;
        Contact contact;

        ContactHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.contact, parent, false));
            itemView.setOnClickListener(this);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            mContactName = itemView.findViewById(R.id.contact_name);
            mPhoneNumber = itemView.findViewById(R.id.phone_number);
        }

        @Override
        public void onClick(View view) {
            int previousPos = currentPos;
            currentPos = getAdapterPosition();
            if(parentActivity.getLayoutType() == LayoutType.SINGLE_PANE) {
                Intent detailIntent = new Intent(getContext(), ContactDetailActivity.class);
                detailIntent.putExtra(ARG_CONTACT, contact);
                parentActivity.startActivity(detailIntent);
            }
            else {
                setCurrentDetail(contact);
                selectContact(currentPos);
                deselectContact(previousPos);
            }
        }
    }

    private void setCurrentDetail(Contact contact) {
        ImageButton detailMenu = parentActivity.findViewById(R.id.contact_detail_menu);
        ImageButton editContact = parentActivity.findViewById(R.id.edit_contact);
        ImageButton callContact = parentActivity.findViewById(R.id.call_contact);
        if(contact != null) {
            ContactDetailFragment contactDetail = ContactDetailFragment.newInstance(contact);
            parentActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contact_detail_fragment_container, contactDetail)
                    .commitAllowingStateLoss(); //fără chestia asta îmi dă un Caused by:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState cînd înlocuiesc fragmentul detail după adăugarea unui
            //contact nou. Soluția: https://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
        }
        else {
            //celelalte butoane nu pot să fie vizibile deoarece ștergerea unui contact nu se poate face cu selectMode on
            detailMenu.setVisibility(View.GONE);
            editContact.setVisibility(View.GONE);
            callContact.setVisibility(View.GONE);

            Fragment detailFragment = parentActivity.getSupportFragmentManager().findFragmentById(R.id.contact_detail_fragment_container);
            if(detailFragment != null) //dacă aplicația începe fără niciun contact detailFragment va fi null
                parentActivity.getSupportFragmentManager().beginTransaction().remove(detailFragment).commit();
        }
    }

    class ContactsAdapter extends RecyclerView.Adapter<ContactHolder> {
        private List<Contact> contacts;

        ContactsAdapter(List<Contact> contactList){
            List<Contact> updatedList = new ArrayList<>();
            for(Contact contact : contactList) {
                int color;
                if(contactsColors.containsKey(contact.getId()))
                    color = contactsColors.get(contact.getId());
                else {
                    if(colorPointer == Util.colorList.size() - 1)
                        colorPointer = 0;
                    color = Util.colorList.get(colorPointer++);
                    contactsColors.put(contact.getId(), color);
                }
                contact.setColor(color);
                updatedList.add(contact);
            }
            contacts = updatedList;
        }

        @Override
        @NonNull
        public ContactHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parentActivity);
            return new ContactHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactHolder holder, int position) {
            Contact contact = contacts.get(position);
                if (contact.getPhotoUri() != null) {
                    holder.contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
                    holder.contactPhoto.setImageURI(contact.getPhotoUri());
                } else {
                    if (contact.isPrivateNumber()) {
                        holder.contactPhoto.setImageResource(R.drawable.incognito);
                        holder.mPhoneNumber.setVisibility(View.GONE);
                    }
                    else {
                        holder.contactPhoto.setImageResource(R.drawable.user_contact);
                        //PorteDuffColorFilter ia întotdeauna un aRGB. modificarea culorii funcționează în felul
                        //următor: user_contact.xml are în centru culoarea #E6E6E6 (luminozitate 230), mai mare decît
                        //toate culorile din listă. La margine are negru,luminozitate mai mică decît toate culorile
                        //din listă. Aplicînd modul LIGHTEN (https://developer.android.com/reference/android/graphics/PorterDuff.Mode.html#LIGHTEN)
                        // se inlocuiește totdeauna culoarea de pe margine și este păstrată culoarea din centru.
                        //Alternativa ar fi https://github.com/harjot-oberai/VectorMaster .
                        holder.contactPhoto.setColorFilter(new
                                PorterDuffColorFilter(contact.getColor(), PorterDuff.Mode.LIGHTEN));
                    }
                }

            holder.mContactName.setText(contact.getContactName());
            holder.contact = contact;
            if(!contact.isPrivateNumber())
                holder.mPhoneNumber.setText(contact.getPhoneNumber());
            if(parentActivity.getLayoutType() == LayoutType.DOUBLE_PANE)
                holder.mPhoneNumber.setVisibility(View.GONE);

            //codul de mai jos este necesar pentru că în momentul în care fragmentul pornește sau repornește
            // trebuie să marcheze contactul activ: primul la pornire, currentPos la repornire. Folosește o versiune
            //privată a selectContact() care ia ca parametru un View deaorece la pornire lista cu contacte a
            // adapterului nu este disponibilă.
            if(parentActivity.getLayoutType() == LayoutType.DOUBLE_PANE) {
                if(position == currentPos)
                    selectContactWithView(holder.itemView);
            }
         }

        Contact getItem(int position) {
            return contacts.get(position);
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

    }
}
