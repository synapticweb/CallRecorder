package net.synapticweb.callrecorder.databases;

import android.provider.BaseColumns;

public class RecordingsContract {
    private RecordingsContract(){}

    public static class Recordings implements BaseColumns {
        public static final String TABLE_NAME = "recordings";

        public static final String COLUMN_NAME_PHONE_NUM_ID = "phone_num_id";
        public static final String COLUMN_NAME_INCOMING = "incoming";
        public static final String COLUMN_NAME_PATH = "path";
        public static final String COLUMN_NAME_START_TIMESTAMP = "start_timestamp";
        public static final String COLUMN_NAME_END_TIMESTAMP = "end_timestamp";
    }
}
