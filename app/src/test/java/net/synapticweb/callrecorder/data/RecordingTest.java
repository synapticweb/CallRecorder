package net.synapticweb.callrecorder.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecordingTest {
    @Test
    public void hasIllegalCharacter_IllegalChar_returnsTrue() {
        String illegals = "~!@#$%^&*()+=?|\\':;\"><{}[]/";
        for(int i = 0; i < illegals.length(); ++i) {
            char c = illegals.charAt(i);
            assertTrue(Recording.hasIllegalChar(Character.toString(c)));
        }
    }

    @Test
    public void hasIllegalCharacter_legalChars_returnsFalse() {
        String legalChars = "abcde10ABC.-";
        assertFalse(Recording.hasIllegalChar(legalChars));
    }

}