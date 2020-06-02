package net.synapticweb.callrecorder.data;

import java.util.ArrayList;
import java.util.List;

public class FakeRepository implements Repository {
    private List<Contact> contacts = new ArrayList<>();
    private List<Recording> recordings = new ArrayList<>();

    private <T> Long getNextId(List<T> list) {
        if(list.size() > 0) {
            T last = list.get(list.size() - 1);
            if (last instanceof Contact)
                return ((Contact) last).getId() + 1;
            else
                return ((Recording) last).getId() + 1;
        }
        else
            return 1L;
    }

    void addContact(String name, String number) {
        Contact contact = new Contact(getNextId(contacts), number, name, null, null);
        contacts.add(contact);
    }

    @Override
    public List<Contact> getAllContacts() {
        return contacts;
    }

    @Override
    public void getAllContacts(LoadContactsCallback callback) {

    }

    @Override
    public Long getHiddenNumberContactId() {
        return null;
    }

    @Override
    public void insertContact(Contact contact) {

    }

    @Override
    public void updateContact(Contact contact) {

    }

    @Override
    public void deleteContact(Contact contact) {

    }

    @Override
    public void getRecordings(Contact contact, LoadRecordingsCallback callback) {

    }

    @Override
    public List<Recording> getRecordings(Contact contact) {
        return null;
    }

    @Override
    public void insertRecording(Recording recording) {

    }

    @Override
    public void updateRecording(Recording recording) {

    }

    @Override
    public void deleteRecording(Recording recording) {

    }
}
