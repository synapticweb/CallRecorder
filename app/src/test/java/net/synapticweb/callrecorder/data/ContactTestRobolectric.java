package net.synapticweb.callrecorder.data;

import android.content.ContentResolver;
import android.content.pm.ProviderInfo;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.data.FakePhoneLookupProvider.ContactData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ContentProviderController;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//@RunWith(RobolectricTestRunner.class)
@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.P) //https://stackoverflow.com/questions/56821193/does-robolectric-require-java-9
public class ContactTestRobolectric {
    private static final String GOOD_NUMBER = "0736192257";
    private static final String BAD_NUMBER = "0744567889";
    private static final String CONTACT_NAME = "contact name";
    private static final int CONTACT_TYPE = Util.UNKNOWN_TYPE_PHONE_CODE;
    private static final String PHOTO_URI = "photo_uri";

    //Cod vechi, ideea de la care am pornit: https://github.com/juanmendez/android_dev/blob/master/16.observers/00.magazineAppWithRx/app/src/test/java/ContentProviderTest.java
    //https://stackoverflow.com/questions/18290864/create-a-cursor-from-hardcoded-array-instead-of-db
    //Ceea ce m-a lămurit într-un final: https://stackoverflow.com/questions/18022923/robolectric-contentprovider-testing
    @Test
    public void queryNumberInPhoneContacts_return_ok() {
        ContentResolver resolver = ApplicationProvider.getApplicationContext().getContentResolver();
        ProviderInfo info = new ProviderInfo();
        info.authority = FakePhoneLookupProvider.AUTHORITY;
        ContentProviderController<FakePhoneLookupProvider> controller =
                Robolectric.buildContentProvider(FakePhoneLookupProvider.class).create(info);

        FakePhoneLookupProvider provider = controller.get();

        provider.addContact(new ContactData(CONTACT_NAME, GOOD_NUMBER, CONTACT_TYPE, PHOTO_URI));
        Contact contact = Contact.queryNumberInPhoneContacts(GOOD_NUMBER, resolver);
        assertNotNull(contact);
        assertThat(contact.getContactName(), is(CONTACT_NAME));
        assertThat(contact.getPhoneNumber(), is(GOOD_NUMBER));
        assertThat(contact.getPhoneTypeCode(), is(CONTACT_TYPE));
        assertThat(contact.getPhotoUri(), is(Uri.parse(PHOTO_URI)));

        contact = Contact.queryNumberInPhoneContacts(BAD_NUMBER, resolver);
        assertNull(contact);
    }

    @Test
    public void queryNumberInPhoneContacts_with_mock() {
        ContentResolver resolver = mock(ContentResolver.class);
        Uri goodLookupUri = Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(GOOD_NUMBER));
        Uri badLookupUri = Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(BAD_NUMBER));
        String[] columns = new String[] { android.provider.ContactsContract.PhoneLookup.NUMBER,
                android.provider.ContactsContract.PhoneLookup.TYPE,
                android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME,
                android.provider.ContactsContract.PhoneLookup.PHOTO_URI };
        MatrixCursor emptyCursor = new MatrixCursor(columns);
        MatrixCursor dataCursor = new MatrixCursor(columns);
        dataCursor.addRow(new Object[] {GOOD_NUMBER, Util.UNKNOWN_TYPE_PHONE_CODE, "NAME", null});

        when(resolver.query(eq(goodLookupUri), any(), any(), any(), any()))
                .thenReturn(dataCursor);

        when(resolver.query(eq(badLookupUri), any(), any(), any(), any()))
                .thenReturn(emptyCursor);

        Contact contact = Contact.queryNumberInPhoneContacts(GOOD_NUMBER, resolver);
        assertNotNull(contact);
        contact = Contact.queryNumberInPhoneContacts(BAD_NUMBER, resolver);
        assertNull(contact);
    }
}