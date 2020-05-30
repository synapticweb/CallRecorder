package net.synapticweb.callrecorder.contactslist.di;

import net.synapticweb.callrecorder.contactslist.ContactsListContract;
import net.synapticweb.callrecorder.contactslist.ContactsListFragment;

import dagger.Module;
import dagger.Provides;

@Module
public class ViewModule {
    private ContactsListFragment fragment;
    public ViewModule(ContactsListFragment fragment) {
        this.fragment = fragment;
    }

    @Provides
    public ContactsListContract.View provideView() {
        return fragment;
    }
}
