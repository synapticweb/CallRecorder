package net.synapticweb.callrecorder.contactslist;

import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsRepository;

import java.util.List;

public class ContactsListPresenter implements ContactsListContract.ContactsListPresenter {

    private ContactsListContract.View view;

    ContactsListPresenter(ContactsListContract.View view) {
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
        view.replaceDetailFragment(contact);
    }

    @Override
    public void openContactDetails(Contact contact) {
        if(view.isSinglePaneLayout())
            view.startContactDetailActivity(contact);
        else {
            setCurrentDetail(contact);
        }
    }
}
