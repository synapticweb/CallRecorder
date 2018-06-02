package net.synapticweb.callrecorder.databases;

import android.provider.BaseColumns;

public class ListenedContract {
    private ListenedContract(){}

    public static class Listened implements BaseColumns {
        public static final String TABLE_NAME = "listened";

        public static final String COLUMN_NAME_NUMBER_ID = "number_id";
        public static final String COLUMN_NAME_NUMBER = "phone_number";
    }
}
