/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import android.app.Activity;
import android.content.Intent;
import net.synapticweb.callrecorder.data.Contact;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

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
        void setContactDeleted(boolean deleted);
    }

    interface ContactsListPresenter {
        void loadContacts();
        void manageContactDetails(Contact contact, int previousSelectedPosition, int currentSelectedPosition);
        void setCurrentDetail(Contact contact);
        void addNewContact();
        void onAddContactResult(Intent intent);
    }
}
