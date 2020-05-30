package net.synapticweb.callrecorder.contactdetail.di;

import net.synapticweb.callrecorder.contactdetail.ContactDetailContract.Presenter;
import net.synapticweb.callrecorder.contactdetail.ContactDetailPresenter;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class PresenterModule {
    @Binds
    abstract Presenter providePresenter(ContactDetailPresenter presenter);
}
