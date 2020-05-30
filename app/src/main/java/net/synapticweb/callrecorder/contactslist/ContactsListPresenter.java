/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;

import net.synapticweb.callrecorder.data.Repository;
import net.synapticweb.callrecorder.di.FragmentScope;

import androidx.annotation.NonNull;
import javax.inject.Inject;

@FragmentScope
public class ContactsListPresenter implements ContactsListContract.Presenter {
    @NonNull private ContactsListContract.View view;
    private Repository repository;

    @Inject
    ContactsListPresenter(@NonNull ContactsListContract.View view, Repository repository) {
        this.view = view;
        this.repository = repository;
    }

    @Override
    public void loadContacts() {
        repository.getAllContacts(contacts -> view.showContacts(contacts));
    }

}
