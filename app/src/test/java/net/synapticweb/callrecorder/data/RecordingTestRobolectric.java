package net.synapticweb.callrecorder.data;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@Config(sdk = Build.VERSION_CODES.P)
public class RecordingTestRobolectric {
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext(); //nu merge în @BeforeClass
    }

    @Test
    public void isSavedInPrivateSpace_PrivatePath_ReturnsTrue() {
        String privatePath = context.getFilesDir() + "/testpath";
        Recording recording = new Recording();
        recording.setPath(privatePath);
        assertTrue(recording.isSavedInPrivateSpace(context));
    }

    @Test
    public void isSavedInPrivateSpace_PublicPath_ReturnsFalse() {
        String publicPath = context.getCacheDir() + "/testpath";
        Recording recording = new Recording();
        recording.setPath(publicPath);
        assertFalse(recording.isSavedInPrivateSpace(context));
    }

    @Test
    public void getName_NameSet_returnsFileName() {
        String path = context.getFilesDir() + "/testfile.aac";
        Recording recording = new Recording();
        recording.setIsNameSet(true);
        recording.setPath(path);
        assertThat(recording.getName(), is("testfile"));
    }

}