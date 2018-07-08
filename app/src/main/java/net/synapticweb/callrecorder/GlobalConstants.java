package net.synapticweb.callrecorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GlobalConstants {
    static final int SQLITE_TRUE = 1;
    static final int SQLITE_FALSE = 0;
    static final int UNKNOWN_TYPE_PHONE_CODE = -1;

    //https://stackoverflow.com/questions/2760995/arraylist-initialization-equivalent-to-array-initialization
    static final List<PhoneTypeContainer> PHONE_TYPES = new ArrayList<>(Arrays.asList(
            new PhoneTypeContainer(1, "Home"),
            new PhoneTypeContainer(2, "Mobile"),
            new PhoneTypeContainer(3, "Work"),
            new PhoneTypeContainer(-1, "Unknown")));
}
