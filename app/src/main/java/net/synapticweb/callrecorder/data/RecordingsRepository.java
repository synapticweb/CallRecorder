/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.synapticweb.callrecorder.CrApp;

import java.util.ArrayList;
import java.util.List;

public class RecordingsRepository {
    public interface loadRecordingsCallback {
        void onRecordingsLoaded(List<Recording> recordings);
    }

    public static void getRecordings(Contact contact, loadRecordingsCallback callback) {
        CallRecorderDbHelper mDbHelper = new CallRecorderDbHelper(CrApp.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<Recording> list =  new ArrayList<>();

        Cursor cursor = db.query(RecordingsContract.Recordings.TABLE_NAME,
                null, RecordingsContract.Recordings.COLUMN_NAME_CONTACT_ID + "=" + contact.getId(), null, null, null, null);

        while(cursor.moveToNext())
        {
            Recording recording = new Recording(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings._ID)),
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_CONTACT_ID)),
                    cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_PATH)),
                    cursor.getInt(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_INCOMING)) == 1,
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_START_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP)),
                    cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_FORMAT)),
                    cursor.getInt(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_IS_NAME_SET)) == 1,
                    cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_MODE)));
            list.add(recording);
        }
        cursor.close();
        callback.onRecordingsLoaded(list);
    }
}
