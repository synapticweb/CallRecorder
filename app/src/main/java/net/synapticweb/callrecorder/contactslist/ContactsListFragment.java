package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
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
    private int currentPos = 0;
    private boolean hasRestarted = false;
    private final static String CURRENT_POS_KEY = "current_pos";

    public void resetDetailFragment() {
        currentPos = 0;
    }

    @Override
    public void onResume(){
        super.onResume();
        presenter.loadContacts();
        if(!isSinglePaneLayout() && !hasRestarted) {
            if(adapter.getItemCount() > 0)
                presenter.setCurrentDetail(adapter.getItem(currentPos));
            else
                presenter.setCurrentDetail(null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_POS_KEY, currentPos);
    }

    @Override
    public void startContactDetailActivity(Contact contact) {
        Intent detailIntent = new Intent(getContext(), ContactDetailActivity.class);
        detailIntent.putExtra("contact", contact);
        startActivity(detailIntent);
    }

    public boolean isSinglePaneLayout() {
        Activity parentActivity = getActivity();
        return (parentActivity != null &&
                parentActivity.findViewById(R.id.contact_detail_fragment_container) == null);
    }

    public void replaceDetailFragment(Contact contact) {
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if(parentActivity == null)
            return ;
        TextView title = parentActivity.findViewById(R.id.actionbar_select_title);
        ImageButton detailMenu = parentActivity.findViewById(R.id.phone_number_detail_menu);
        if(contact != null) {
            title.setText(contact.getContactName());
            detailMenu.setVisibility(View.VISIBLE);

            ContactDetailFragment contactDetail = ContactDetailFragment.newInstance(contact);
            parentActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contact_detail_fragment_container, contactDetail)
                    .commitAllowingStateLoss(); //fără chestia asta îmi dă un Caused by:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState cînd înlocuiesc fragmentul detail după adăugarea unui
            //contact nou. Soluția: https://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
        }
        else {
            title.setText(parentActivity.getResources().getString(R.string.app_name));
            detailMenu.setVisibility(View.GONE);

            Fragment detailFragment = parentActivity.getSupportFragmentManager().findFragmentById(R.id.contact_detail_fragment_container);
            if(detailFragment != null) //dacă aplicația începe fără niciun contact detailFragment va fi null
                parentActivity.getSupportFragmentManager().beginTransaction().remove(detailFragment).commit();
        }
    }

    @Override
    public void showContacts(List<Contact> contacts) {
        adapter.replaceData(contacts);
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
        RecyclerView listenedPhones = (RecyclerView) inflater.inflate(R.layout.list_contacts_fragment, container, false);
        listenedPhones.setLayoutManager(new LinearLayoutManager(getActivity()));
        listenedPhones.setAdapter(adapter);
        return listenedPhones;
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
            currentPos = getAdapterPosition();
            presenter.openContactDetails(contact);
        }
    }

    class ListenedAdapter extends RecyclerView.Adapter<PhoneHolder> {
        private List<Contact> contacts;
        ListenedAdapter(List<Contact> list){
            contacts = list;
        }

        void replaceData(List<Contact> contacts) {
            this.contacts = contacts;
            notifyDataSetChanged();
        }

        @Override
        @NonNull
        public PhoneHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new PhoneHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull PhoneHolder holder, int position) {
            Contact contact = contacts.get(position);

            if(contact.getPhotoUri() != null) {
                holder.contactPhoto.setImageURI(null); //cînd se schimbă succesiv 2 poze făcute de cameră se folosește același fișier și optimizările android fac necesar acest hack pentru a obține refresh-ul pozei
                holder.contactPhoto.setImageURI(contact.getPhotoUri());
            }
            else {
                if(contact.isPrivateNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_yellow);
                else if(contact.isUnkownNumber())
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_red);
                else
                    holder.contactPhoto.setImageResource(R.drawable.user_contact_blue);
            }

            holder.mContactName.setText(contact.getContactName());
            holder.contact = contact;
            if(!contact.isPrivateNumber())
                holder.mPhoneNumber.setText(contact.getPhoneNumber());
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
