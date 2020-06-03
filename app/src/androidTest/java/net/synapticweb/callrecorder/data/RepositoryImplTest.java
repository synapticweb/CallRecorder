package net.synapticweb.callrecorder.data;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.recorder.Recorder;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;


public class RepositoryImplTest {
    private static final String CONTACT_NUMBER = "0744355665";
    private static final String CONTACT_NAME = "Contact name";
    private static String CONTACT_PHOTO_URI;
    private static final int CONTACT_PHONE_TYPE = Util.UNKNOWN_TYPE_PHONE_CODE;

    private static String RECORDING_PATH;
    private static final boolean RECORDING_INCOMING = true;
    private static final long RECORDING_START_TIMESTAMP = System.currentTimeMillis();
    private static final long RECORDING_END_TIMESTAMP = System.currentTimeMillis() + 60000;
    private static final boolean RECORDING_IS_NAME_SET = false;
    private static final String RECORDING_FORMAT = Recorder.AAC_MEDIUM_FORMAT;
    private static final String RECORDING_MODE = "mono";
    private static final String RECORDING_SOURCE = "Voice recognition";

    private static Context context;
    private RepositoryImpl repository;

    @BeforeClass
    public static void setupClass() {
        context = ApplicationProvider.getApplicationContext();
        CONTACT_PHOTO_URI = context.getFilesDir() + "/test_photo.jpg";
        RECORDING_PATH = context.getFilesDir() + "/test_recording.aac";
    }

    @Before
    public void setupTest() {
        repository = new RepositoryImpl(context, null); //passing null creates in memory db.
    }

    @After
    public void tearOff() {
        repository.closeDb();
    }

    @Test
    public void insert_retrieve_many_contacts() {
        List<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI, CONTACT_PHONE_TYPE));
        contacts.add(new Contact(null, CONTACT_NUMBER + "11", CONTACT_NAME + "aa",
                context.getFilesDir() + "test_photo2.jpg", CONTACT_PHONE_TYPE + 1));

        for(Contact contact : contacts)
            repository.insertContact(contact);

        List<Contact> retrievedContacts = repository.getAllContacts();
        assertThat(contacts, is(retrievedContacts));
    }


    @Test
    public void insert_update_contact() {
        String newNumber = "07444556444";
        String newName = "New contact name";
        String newUri = context.getFilesDir() + "new_test_photo.jpg";
        int newType = Util.PHONE_TYPES.get(1).getTypeCode();
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        repository.insertContact(contact);
        Long id = contact.getId();

        contact.setContactName(newName);
        contact.setPhoneNumber(newNumber);
        contact.setPhotoUri(newUri);
        contact.setPhoneType(newType);

        repository.updateContact(contact);
        Contact retrieved = repository.getContact(id);

        assertThat(retrieved.getPhoneNumber(), is(newNumber));
        assertThat(retrieved.getContactName(), is(newName));
        assertThat(retrieved.getPhoneTypeCode(), is(newType));
        assertThat(retrieved.getPhotoUri().toString(), is(newUri));
    }

    @Test
    public void updateUninsertedContact_ThrowsException() {
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        assertThatThrownBy(() -> repository.updateContact(contact))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void insert_delete() {
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        repository.insertContact(contact);
        Long id = contact.getId();
        Contact retrieved = repository.getContact(id);
        repository.deleteContact(retrieved);
        retrieved = repository.getContact(id);
        assertNull(retrieved);
    }

    @Test
    public void deleteUnInsertedContact_ThrowsException() {
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        assertThatThrownBy(() -> repository.deleteContact(contact))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void insertHiddenContact_getHiddenId() {
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        contact.setIsPrivateNumber();
        repository.insertContact(contact);
        Long hiddenId = repository.getHiddenNumberContactId();
        assertThat(hiddenId, is(1L));
        repository.deleteContact(contact);
        hiddenId = repository.getHiddenNumberContactId();
        assertNull(hiddenId);
    }

    @Test
    public void insert_retrieve_many_records() {
        List<Recording> recordings = new ArrayList<>();
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        repository.insertContact(contact);
        long contactId = contact.getId();

        recordings.add(new Recording(null, contactId, RECORDING_PATH,
                RECORDING_INCOMING, RECORDING_START_TIMESTAMP, RECORDING_END_TIMESTAMP,
                RECORDING_FORMAT, RECORDING_IS_NAME_SET, RECORDING_MODE, RECORDING_SOURCE));
        recordings.add(new Recording(null, contactId, RECORDING_PATH + "aa",
                false, RECORDING_START_TIMESTAMP, RECORDING_END_TIMESTAMP,
                Recorder.WAV_FORMAT, true, "stereo", "Voice call"));

        for(Recording recording : recordings)
            repository.insertRecording(recording);

        List<Recording> retrievedRecordings = repository.getRecordings(contact);
        assertThat(recordings, is(retrievedRecordings));
    }

    @Test
    public void insert_update_recording() {
        Contact contact = new Contact(null, CONTACT_NUMBER, CONTACT_NAME, CONTACT_PHOTO_URI,
                CONTACT_PHONE_TYPE);
        repository.insertContact(contact);
        long contactId = contact.getId();

        Recording recording = new Recording(null, contactId, RECORDING_PATH,
                RECORDING_INCOMING, RECORDING_START_TIMESTAMP, RECORDING_END_TIMESTAMP,
                RECORDING_FORMAT, RECORDING_IS_NAME_SET, RECORDING_MODE, RECORDING_SOURCE);

        repository.insertRecording(recording);
        long recordingId = recording.getId();

        long newContactId = 2L;
        String newPath = RECORDING_PATH + "aa";
        boolean newIncoming = false;
        long newStartTimestamp = System.currentTimeMillis();
        long newEndTimestamp = newStartTimestamp + 60000;
        boolean newIsNameSet = true;
        String newFormat = Recorder.AAC_BASIC_FORMAT;
        String newMode = "stereo";
        String newSource = "mic";

        recording.setContactId(newContactId);
        recording.setPath(newPath);
        recording.setIncoming(newIncoming);
        recording.setStartTimestamp(newStartTimestamp);
        recording.setEndTimestamp(newEndTimestamp);
        recording.setIsNameSet(newIsNameSet);
        recording.setFormat(newFormat);
        recording.setMode(newMode);
        recording.setSource(newSource);

        repository.updateRecording(recording);
        Recording retrieved = repository.getRecording(recordingId);

        assertThat(retrieved.getContactId(), is(newContactId));
        assertThat(retrieved.getPath(), is(newPath));
        assertThat(retrieved.isIncoming(), is(newIncoming));
        assertThat(retrieved.getStartTimestamp(), is(newStartTimestamp));
        assertThat(retrieved.getEndTimestamp(), is(newEndTimestamp));
        assertThat(retrieved.getIsNameSet(), is(newIsNameSet));
        assertThat(retrieved.getFormat(), is(newFormat));
        assertThat(retrieved.getMode(), is(newMode));
        assertThat(retrieved.getSource(), is(newSource));
    }

    @Test
    public void updateUninsertedRecording_ThrowsException() {
        Recording recording = new Recording(null, 1L, RECORDING_PATH,
                RECORDING_INCOMING, RECORDING_START_TIMESTAMP, RECORDING_END_TIMESTAMP,
                RECORDING_FORMAT, RECORDING_IS_NAME_SET, RECORDING_MODE, RECORDING_SOURCE);
        assertThatThrownBy(() -> repository.updateRecording(recording))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void insert_delete_recording() {
        Recording recording = new Recording(null, 1L, RECORDING_PATH,
                RECORDING_INCOMING, RECORDING_START_TIMESTAMP, RECORDING_END_TIMESTAMP,
                RECORDING_FORMAT, RECORDING_IS_NAME_SET, RECORDING_MODE, RECORDING_SOURCE);
        repository.insertRecording(recording);
        Long id = recording.getId();
        repository.deleteRecording(recording);
        assertNull(repository.getRecording(id));
    }

    @Test
    public void deleteUninsertedRecording_ThrowsException() {
        Recording recording = new Recording(null, 1L, RECORDING_PATH,
                RECORDING_INCOMING, RECORDING_START_TIMESTAMP, RECORDING_END_TIMESTAMP,
                RECORDING_FORMAT, RECORDING_IS_NAME_SET, RECORDING_MODE, RECORDING_SOURCE);
        assertThatThrownBy(() -> repository.deleteRecording(recording))
                .isInstanceOf(IllegalStateException.class);
    }
}