package net.synapticweb.callrecorder.di;

import android.content.Context;

import net.synapticweb.callrecorder.contactdetail.EditContactActivity;
import net.synapticweb.callrecorder.contactdetail.di.ContactDetailComponent;
import net.synapticweb.callrecorder.contactslist.di.ContactsListComponent;
import net.synapticweb.callrecorder.recorder.RecorderService;

import javax.inject.Singleton;
import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = { RepositoryModule.class, AppSubcomponents.class })
public interface AppComponent {
    @Component.Factory
    interface Factory {
        AppComponent create(@BindsInstance Context context);
    }

    void inject(EditContactActivity activity);
    void inject(RecorderService service);
    ContactsListComponent.Factory contactsListComponent();
    ContactDetailComponent.Factory contactDetailComponent();
}
