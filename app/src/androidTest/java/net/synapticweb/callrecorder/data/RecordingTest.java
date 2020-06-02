package net.synapticweb.callrecorder.data;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.*;

public class RecordingTest {
    private static final String FILE_NAME = "testfile";
    private static String RANDOM_FILE_PATH;
    private static String DESTINATION_FOLDER;
    private static final int FILE_SIZE = 1048576;
    private RepositoryImpl repository;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        RANDOM_FILE_PATH = new File(context.getFilesDir() + "/" + FILE_NAME).getAbsolutePath();
        DESTINATION_FOLDER = context.getExternalFilesDir(null).getAbsolutePath();
        repository = new RepositoryImpl(context, null);
    }

    @After
    public void removeFile() {
        if(new File(DESTINATION_FOLDER + "/" + FILE_NAME).exists())
            new File(DESTINATION_FOLDER + "/" + FILE_NAME).delete();
    }

    private void createRandomFile() {
        try {
            OutputStream os = new FileOutputStream(RANDOM_FILE_PATH);
            for(int i = 0; i < FILE_SIZE; ++i) {
                int number = ThreadLocalRandom.current().nextInt(0, 255 + 1);
                os.write(number);
            }
            os.flush();
        }
        catch (IOException ignored) {}
    }

    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    private String computeFileSha1(String path) {
        String hash = "";
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            InputStream input = new FileInputStream(path);
            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }
            hash = byteArrayToHexString(sha1.digest());
        }
        catch (Exception ignored)  {}
        return hash;
    }

    @Test
    public void move() {
        createRandomFile();
        String hash1 = computeFileSha1(RANDOM_FILE_PATH);

        Recording recording = new Recording();
        recording.setPath(RANDOM_FILE_PATH);
        repository.insertRecording(recording);
        Long id = recording.getId();

        try {
            recording.move(repository, DESTINATION_FOLDER, null, FILE_SIZE);
        }
        catch (IOException ignored) {}

        assertTrue(new File(DESTINATION_FOLDER + "/" + FILE_NAME).exists());
        assertFalse(new File(RANDOM_FILE_PATH).exists());
        String hash2 = computeFileSha1(DESTINATION_FOLDER + "/" + FILE_NAME);
        assertThat(hash1, is(hash2));

        assertThat(recording.getPath(), is(DESTINATION_FOLDER + "/" + FILE_NAME));
        recording = repository.getRecording(id);
        assertThat(recording.getPath(), is(DESTINATION_FOLDER + "/" + FILE_NAME));
    }
}