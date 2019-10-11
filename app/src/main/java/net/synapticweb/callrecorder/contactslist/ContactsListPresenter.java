/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import android.view.View;
import android.widget.ImageButton;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsRepository;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class ContactsListPresenter implements ContactsListContract.ContactsListPresenter {
    @NonNull private ContactsListContract.View view;

    ContactsListPresenter(@NonNull ContactsListContract.View view) {
        this.view = view;
    }

    @Override
    public void loadContacts() {
        ContactsRepository.getContacts(new ContactsRepository.LoadContactsCallback() {
            @Override
            public void onContactsLoaded(List<Contact> contacts) {
                view.showContacts(contacts);
            }
        });
    }

    @Override
    public void setCurrentDetail(Contact contact) {
        Fragment fragment = (Fragment) view;
        AppCompatActivity parentActivity = (AppCompatActivity) fragment.getActivity();
        if(parentActivity == null)
            return ;

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

    @Override
    public void manageContactDetails(Contact contact, int previousSelectedPosition, int currentSelectedPosition) {
        if(view.isSinglePaneLayout())
            view.startContactDetailActivity(contact);
        else {
            setCurrentDetail(contact);
            View currentSelectedView = view.getContactsRecycler().getLayoutManager().findViewByPosition(currentSelectedPosition);
            View previousSelectedView = view.getContactsRecycler().getLayoutManager().findViewByPosition(previousSelectedPosition);
            view.selectContact(currentSelectedView);
            view.deselectContact(previousSelectedView);
            view.getContactsAdapter().notifyDataSetChanged();
        }
    }

}
