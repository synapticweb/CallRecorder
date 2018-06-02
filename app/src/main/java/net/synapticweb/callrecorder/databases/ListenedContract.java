package net.synapticweb.callrecorder.databases;

import android.provider.BaseColumns;

public class ListenedContract {
    private ListenedContract(){}

    public static class Listened implements BaseColumns {
        public static final String TABLE_NAME = "listened";

        public static final String COLUMN_NAME_NUMBER_ID = "number_id";
        public static final String COLUMN_NAME_LOOKUP_KEY = "lookup_key";
        public static final String COLUMN_NAME_NUMBER_IF_UNKNOWN = "phone_number_if_unknown";
    }
}
