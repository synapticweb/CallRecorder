package net.synapticweb.callrecorder.contactdetail.di;

import net.synapticweb.callrecorder.contactdetail.ContactDetailContract;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;

import dagger.Module;
import dagger.Provides;

@Module
public class ViewModule {
    private ContactDetailFragment fragment;
    public ViewModule(ContactDetailFragment fragment) {
        this.fragment = fragment;
    }

    @Provides
    public ContactDetailContract.View provideView() {
        return fragment;
    }
}
