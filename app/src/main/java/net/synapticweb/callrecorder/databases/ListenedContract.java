package net.synapticweb.callrecorder.databases;

import android.provider.BaseColumns;

public class ListenedContract {
    private ListenedContract(){}

    public static class Listened implements BaseColumns {
        public static final String TABLE_NAME = "listened";

        public static final String COLUMN_NAME_UNKNOWN_PHONE = "unknown_phone";
        public static final String COLUMN_NAME_CONTACT_PHOTO_URI = "contact_photo_uri";
        public static final String COLUMN_NAME_PHONE_TYPE = "phone_type";
        public static final String COLUMN_NAME_CONTACT_NAME = "contact_name";
        public static final String COLUMN_NAME_PHONE_NUMBER = "phone_number";
    }
}
