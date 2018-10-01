package net.synapticweb.callrecorder.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.synapticweb.callrecorder.CallRecorderApplication;

import java.util.ArrayList;
import java.util.List;

public class RecordingsRepository {
    public interface loadRecordingsCallback {
        void onRecordingsLoaded(List<Recording> recordings);
    }

    public static void getRecordings(Contact contact, loadRecordingsCallback callback) {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(CallRecorderApplication.getInstance());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        List<Recording> list =  new ArrayList<>();

        Cursor cursor = db.query(RecordingsContract.Recordings.TABLE_NAME,
                null, RecordingsContract.Recordings.COLUMN_NAME_PHONE_NUM_ID + "=" + contact.getId(), null, null, null, null);

        while(cursor.moveToNext())
        {
            Recording recording = new Recording(cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings._ID)),
                    cursor.getString(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_PATH)),
                    cursor.getInt(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_INCOMING)) == 1,
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_START_TIMESTAMP)),
                    cursor.getLong(cursor.getColumnIndex(RecordingsContract.Recordings.COLUMN_NAME_END_TIMESTAMP)));
            list.add(recording);
        }
        cursor.close();
        callback.onRecordingsLoaded(list);
    }
}
