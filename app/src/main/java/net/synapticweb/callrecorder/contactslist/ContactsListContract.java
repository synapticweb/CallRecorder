/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactslist;
import net.synapticweb.callrecorder.data.Contact;
import java.util.List;


public interface ContactsListContract {
    interface View {
        void showContacts(List<Contact> contactList);
        void resetCurrentPosition();
    }

    interface Presenter {
        void loadContacts();
    }
}
