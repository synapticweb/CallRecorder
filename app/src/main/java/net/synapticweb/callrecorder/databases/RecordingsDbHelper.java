package net.synapticweb.callrecorder.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.synapticweb.callrecorder.databases.RecordingsContract.*;
import net.synapticweb.callrecorder.databases.ListenedContract.*;

import java.util.List;


public class RecordingsDbHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_RECORDINGS = "CREATE TABLE " +
            Recordings.TABLE_NAME + " (" + Recordings._ID +
            " INTEGER PRIMARY KEY, " + Recordings.COLUMN_NAME_PHONE_NUM_ID + " INTEGER, "
            + Recordings.COLUMN_NAME_INCOMING + " INTEGER, " +
            Recordings.COLUMN_NAME_PATH + " TEXT, " +
            Recordings.COLUMN_NAME_START_TIMESTAMP + " INTEGER, " +
            Recordings.COLUMN_NAME_END_TIMESTAMP + " INTEGER )";

    private static final String SQL_CREATE_LISTENED = "CREATE TABLE " + Listened.TABLE_NAME + " (" + Listened._ID + " INTEGER PRIMARY KEY, " +
            Listened.COLUMN_NAME_NUMBER + " TEXT, " +
            Listened.COLUMN_NAME_UNKNOWN + " INTEGER DEFAULT 0, " +
            "CONSTRAINT no_duplicates UNIQUE(" + Listened.COLUMN_NAME_NUMBER + ") )";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "recordings.db";

    public RecordingsDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_RECORDINGS);
        db.execSQL(SQL_CREATE_LISTENED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}
