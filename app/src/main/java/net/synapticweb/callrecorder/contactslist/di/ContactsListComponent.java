package net.synapticweb.callrecorder.contactslist.di;

import net.synapticweb.callrecorder.contactslist.ContactsListFragment;
import net.synapticweb.callrecorder.di.FragmentScope;
import dagger.Subcomponent;

@Subcomponent(modules = {ViewModule.class, PresenterModule.class})
@FragmentScope
public interface ContactsListComponent {

    @Subcomponent.Factory
    interface Factory {
        ContactsListComponent create(ViewModule module);
    }

    void inject(ContactsListFragment fragment);
}
