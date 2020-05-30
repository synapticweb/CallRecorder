package net.synapticweb.callrecorder.contactslist.di;

import dagger.Binds;
import dagger.Module;
import net.synapticweb.callrecorder.contactslist.ContactsListContract.Presenter;
import net.synapticweb.callrecorder.contactslist.ContactsListPresenter;

@Module
public abstract class PresenterModule {
    @Binds
    public abstract Presenter providePresenter(ContactsListPresenter presenter);
}
