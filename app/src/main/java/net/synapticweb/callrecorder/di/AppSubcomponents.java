package net.synapticweb.callrecorder.di;

import net.synapticweb.callrecorder.contactdetail.di.ContactDetailComponent;
import net.synapticweb.callrecorder.contactslist.di.ContactsListComponent;

import dagger.Module;

@Module(subcomponents = {ContactsListComponent.class, ContactDetailComponent.class})
class AppSubcomponents {
}
