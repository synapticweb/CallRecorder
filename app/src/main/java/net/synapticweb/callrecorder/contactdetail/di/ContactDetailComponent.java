package net.synapticweb.callrecorder.contactdetail.di;

import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.di.FragmentScope;

import dagger.Subcomponent;

@Subcomponent(modules = {ViewModule.class, PresenterModule.class})
@FragmentScope
public interface ContactDetailComponent {
    @Subcomponent.Factory
    interface Factory {
        ContactDetailComponent create(ViewModule module);
    }

    void inject(ContactDetailFragment fragment);
}
