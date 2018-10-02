package net.synapticweb.callrecorder.contactslist;

import android.content.Intent;

import net.synapticweb.callrecorder.data.Contact;
import java.util.List;

public interface ContactsListContract {
    interface View {
        void showContacts(List<Contact> contactList);
        void startContactDetailActivity(Contact contact);
        boolean isSinglePaneLayout();
        void setNewAddedContactId(long id);
        void markSelectedContact(android.view.View previousSelected, android.view.View currentSelected);
    }

    interface ContactsListPresenter {
        void loadContacts();
        void manageContactDetails(Contact contact, android.view.View previousSelected, android.view.View currentSelected);
        void setCurrentDetail(Contact contact);
        void addNewContact();
        void onAddContactResult(Intent intent);
    }
}
