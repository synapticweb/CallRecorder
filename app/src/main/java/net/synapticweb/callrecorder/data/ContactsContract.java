package net.synapticweb.callrecorder.data;

import android.provider.BaseColumns;

public class ContactsContract {
    private ContactsContract(){}

    public static class Contacts implements BaseColumns {
        public static final String TABLE_NAME = "contacts";

        public static final String COLUMN_NAME_NUMBER = "phone_number";
        public static final String COLUMN_NAME_CONTACT_NAME = "contact_name";
        public static final String COLUMN_NAME_PHOTO_URI = "photo_uri";
        public static final String COLUMN_NAME_PHONE_TYPE = "phone_type";
        public static final String COLUMN_NAME_SHOULD_RECORD = "should_record";
        public static final String COLUMN_NAME_PRIVATE_NUMBER = "private_number";
        public static final String COLUMN_NAME_UNKNOWN_NUMBER = "unknown_number";
    }
}
