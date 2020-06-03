package net.synapticweb.callrecorder.data;


import net.synapticweb.callrecorder.Util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;


public class ContactTest {
    private FakeRepository repository = new FakeRepository();

    @Before
    public void setup() {
        repository.addContact("contact", "0744456740");
    }

    @Test
    public void queryNumberInAppContacts_matches_number() {
        String[] numbers = {"0744456740", "0744 456 740", "+40744456740", "+40 744 456740"};
        for(String number : numbers) {
            Contact foundContact = Contact.queryNumberInAppContacts(repository, number);
            assertNotNull(foundContact);
        }

        String numberNotInDb = "0723456880";
        assertNull(Contact.queryNumberInAppContacts(repository, numberNotInDb));
    }

    @Test
    public void setPhoneType_StringParam() {
        Contact contact = new Contact();
        for(Util.PhoneTypeContainer container : Util.PHONE_TYPES) {
            contact.setPhoneType(container.getTypeName());
            assertThat(contact.getPhoneTypeCode(), is(container.getTypeCode()));
            assertThat(contact.getPhoneTypeName(), is(container.getTypeName()));
        }

        contact = new Contact();
        contact.setPhoneType("random string");
        assertThat(contact.getPhoneTypeCode(), is(Util.UNKNOWN_TYPE_PHONE_CODE));
    }

    @Test
    public void setPhoneType_IntegerParam() {
        Contact contact = new Contact();
        for(Util.PhoneTypeContainer container : Util.PHONE_TYPES) {
            contact.setPhoneType(container.getTypeCode());
            assertThat(contact.getPhoneTypeCode(), is(container.getTypeCode()));
            assertThat(contact.getPhoneTypeName(), is(container.getTypeName()));
        }

        contact = new Contact();
        contact.setPhoneType(400);
        assertThat(contact.getPhoneTypeCode(), is(Util.UNKNOWN_TYPE_PHONE_CODE));
    }
}