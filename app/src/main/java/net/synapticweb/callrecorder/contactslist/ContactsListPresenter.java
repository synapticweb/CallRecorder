/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.ContactsRepository;
import java.util.List;
import androidx.annotation.NonNull;


public class ContactsListPresenter implements ContactsListContract.ContactsListPresenter {
    @NonNull private ContactsListContract.View view;

    ContactsListPresenter(@NonNull ContactsListContract.View view) {
        this.view = view;
    }

    @Override
    public void loadContacts() {
        ContactsRepository.getContacts((List<Contact> contacts) -> view.showContacts(contacts));
    }

}
