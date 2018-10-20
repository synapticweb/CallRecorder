package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.synapticweb.callrecorder.contactdetail.ContactDetailActivity;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.R;

import java.util.ArrayList;
import java.util.List;

public class ContactsListFragment extends Fragment implements ContactsListContract.View {
    private ContactsListPresenter presenter;
    private ListenedAdapter adapter;
    private RecyclerView listenedPhones;
    private int currentPos = 0;
    private Long newAddedContactId = null;
    private boolean hasRestarted = false;
    private AppCompatActivity parentActivity;
    private final static String CURRENT_POS_KEY = "current_pos";
    public final static String ARG_CONTACT = "arg_contact";

    @Override
    public Activity getParentActivity() {
        return parentActivity;
    }

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
    public void setNewAddedContactId(long id) {
        newAddedContactId = id;
    }

    public void resetDetailFragment() {
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

    @Override
    public void startContactDetailActivity(Contact contact) {
        Intent detailIntent = new Intent(getContext(), ContactDetailActivity.class);
        detailIntent.putExtra(ARG_CONTACT, contact);
        startActivity(detailIntent);
    }

    public boolean isSinglePaneLayout() {
        return (parentActivity != null &&
                parentActivity.findViewById(R.id.contact_detail_fragment_container) == null);
    }

    //e apelată de callback-ul pasat funcției ContactRepository::getContacts(), la rîndul ei apelată de
    // ContactsListPresenter::loadContacts()
    @Override
    public void showContacts(List<Contact> contacts) {
        adapter = new ListenedAdapter(contacts);
        if(newAddedContactId != null) {
            for(Contact contact : adapter.getData())
                if(contact.getId() == newAddedContactId) {
                    currentPos = adapter.getData().indexOf(contact);
                    break;
                }
            newAddedContactId = null;
        }
        //înainte de asta aveam un adapter.replaceData() care înlocuia lista din adapter și apela notifyData
        //setChanged(). Performanța era mai bună dar problema era că la adăugarea unui contact nou rămînea
        //marcat și cel anterior.
        listenedPhones.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        if(!isSinglePaneLayout() && !hasRestarted) { //dacă ne găsim după un restart al activității
            // datorat rotirii nu trebuie să mai înlocuim fragmentul detaliu, el este deja acolo. Dacă
            // îl înlocuim onResume() al fragmnetului detaliu va fi apelat de 2 ori: prima dată din cauza
            // restartului fragmentului detaliu - cu datele de stare, a doua oară datorită înlocuirii - fără
            //datele de stare.
            if(adapter.getItemCount() > 0)
                presenter.setCurrentDetail(adapter.getItem(currentPos));
            else
                presenter.setCurrentDetail(null);
        }
        TextView noContent = parentActivity.findViewById(R.id.no_content);
        if(adapter.getItemCount() > 0)
            noContent.setVisibility(View.GONE);
        else
            noContent.setVisibility(View.VISIBLE);
    }

    @Override
    public void markSelectedContact(@Nullable  View previousSelected, @Nullable View currentSelected) {
        if(previousSelected != null)
            previousSelected.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        if(currentSelected != null)
            currentSelected.setBackgroundColor(getResources().getColor(R.color.selected_contact));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            currentPos = savedInstanceState.getInt(CURRENT_POS_KEY);
            hasRestarted = true;
        }
        this.adapter = new ListenedAdapter(new ArrayList<Contact>(0));
        this.presenter = new ContactsListPresenter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        listenedPhones = (RecyclerView) inflater.inflate(R.layout.list_contacts_fragment, container, false);
        listenedPhones.setLayoutManager(new LinearLayoutManager(parentActivity));

        if(parentActivity != null) {
            FloatingActionButton fab = parentActivity.findViewById(R.id.add_numbers);
            fab.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    presenter.addNewContact();
                }
            });
        }
        return listenedPhones;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(resultCode == Activity.RESULT_OK && requestCode == ContactsListPresenter.REQUEST_ADD_CONTACT)
            presenter.onAddContactResult(intent);
    }

    public class PhoneHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView contactPhoto;
        TextView mContactName;
        TextView mPhoneNumber;
        Contact contact;

        PhoneHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.listened_phone, parent, false));
            itemView.setOnClickListener(this);
            contactPhoto = itemView.findViewById(R.id.contact_photo);
            mContactName = itemView.findViewById(R.id.contact_name);
            mPhoneNumber = itemView.findViewById(R.id.phone_number);
        }

        @Override
        public void onClick(View view) {
            int previousPos = currentPos;
            currentPos = getAdapterPosition();
            View previousSelected = listenedPhones.getLayoutManager().findViewByPosition(previousPos);
            View currentSelected = listenedPhones.getLayoutManager().findViewByPosition(currentPos);
            presenter.manageContactDetails(contact, previousSelected, currentSelected);
        }
    }

    class ListenedAdapter extends RecyclerView.Adapter<PhoneHolder> {
        private List<Contact> contacts;
        ListenedAdapter(List<Contact> list){
            contacts = list;
        }

        List<Contact> getData() {
            return contacts;
        }

        @Override
        @NonNull
        public PhoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parentActivity);
            return new PhoneHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PhoneHolder holder, int position) {
            Contact contact = contacts.get(position);

            if(!isSinglePaneLayout())
                holder.contactPhoto.setVisibility(View.GONE);
            else {
                if (contact.getPhotoUri() != null) {
                    holder.contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
                    holder.contactPhoto.setImageURI(contact.getPhotoUri());
                } else {
                    if (contact.isPrivateNumber()) {
                        holder.contactPhoto.setImageResource(R.drawable.user_contact_yellow);
                        holder.mPhoneNumber.setVisibility(View.GONE);
                    }
                    else if (contact.isUnkownNumber())
                        holder.contactPhoto.setImageResource(R.drawable.user_contact_red);
                    else
                        holder.contactPhoto.setImageResource(R.drawable.user_contact_blue);
                }
            }

            holder.mContactName.setText(contact.getContactName());
            holder.contact = contact;
            if(!contact.isPrivateNumber())
                holder.mPhoneNumber.setText(contact.getPhoneNumber());
            if(!isSinglePaneLayout())
                holder.mPhoneNumber.setVisibility(View.GONE);

            if(position == currentPos && !isSinglePaneLayout())
                markSelectedContact(null, holder.itemView);
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
