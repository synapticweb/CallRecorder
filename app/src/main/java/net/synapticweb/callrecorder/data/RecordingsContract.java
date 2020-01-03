/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.data;

import android.provider.BaseColumns;

 class RecordingsContract {
    private RecordingsContract(){}

    public static class Recordings implements BaseColumns {
        public static final String TABLE_NAME = "recordings";

        static final String COLUMN_NAME_CONTACT_ID = "phone_num_id";
        static final String COLUMN_NAME_INCOMING = "incoming";
        static final String COLUMN_NAME_PATH = "path";
        static final String COLUMN_NAME_START_TIMESTAMP = "start_timestamp";
        static final String COLUMN_NAME_END_TIMESTAMP = "end_timestamp";
        static final String COLUMN_NAME_IS_NAME_SET = "is_name_set";
        static final String COLUMN_NAME_FORMAT = "format";
        static final String COLUMN_NAME_MODE = "mode";
        static final String COLUMN_NAME_SOURCE = "source";
    }
}
