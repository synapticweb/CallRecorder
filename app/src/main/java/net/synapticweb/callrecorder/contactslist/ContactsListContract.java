package net.synapticweb.callrecorder.contactslist;

import net.synapticweb.callrecorder.data.Contact;
import java.util.List;

public interface ContactsListContract {
    interface View {
        void showContacts(List<Contact> contactList);
        void startContactDetailActivity(Contact contact);
        boolean isSinglePaneLayout();
        void replaceDetailFragment(Contact contact);
    }

    interface ContactsListPresenter {
        void loadContacts();
        void openContactDetails(Contact contact);
        void setCurrentDetail(Contact contact);
    }
}
