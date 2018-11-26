package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.synapticweb.callrecorder.data.Contact;
import java.util.List;

public interface ContactsListContract {
    interface View {
        Activity getParentActivity();
        void showContacts(List<Contact> contactList);
        void startContactDetailActivity(Contact contact);
        boolean isSinglePaneLayout();
        void setNewAddedContactId(long id);
        void selectContact(android.view.View contactSlot);
        void deselectContact(android.view.View contactSlot);
        RecyclerView getContactsRecycler();
        ContactsListFragment.ContactsAdapter getContactsAdapter();

    }

    interface ContactsListPresenter {
        void loadContacts();
        void manageContactDetails(Contact contact, int previousSelectedPosition, int currentSelectedPosition);
        void setCurrentDetail(Contact contact);
        void addNewContact();
        void onAddContactResult(Intent intent);
    }
}
