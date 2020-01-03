/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the Synaptic Call Recorder license. You should have received a copy of the
 * Synaptic Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.synapticweb.callrecorder.data.RecordingsContract.*;
import net.synapticweb.callrecorder.data.ContactsContract.*;


public class CallRecorderDbHelper extends SQLiteOpenHelper {
    private static final String SQL_CREATE_RECORDINGS = "CREATE TABLE " +
            Recordings.TABLE_NAME + " (" + Recordings._ID + " INTEGER NOT NULL PRIMARY KEY, " +
            Recordings.COLUMN_NAME_CONTACT_ID + " INTEGER, "
            + Recordings.COLUMN_NAME_INCOMING + " INTEGER NOT NULL, " +
            Recordings.COLUMN_NAME_PATH + " TEXT NOT NULL, " +
            Recordings.COLUMN_NAME_START_TIMESTAMP + " INTEGER NOT NULL, " +
            Recordings.COLUMN_NAME_END_TIMESTAMP + " INTEGER NOT NULL, " +
            Recordings.COLUMN_NAME_FORMAT + " TEXT NOT NULL, " +
            Recordings.COLUMN_NAME_IS_NAME_SET + " INTEGER  NOT NULL DEFAULT 0, " +
            Recordings.COLUMN_NAME_MODE + " TEXT NOT NULL, " +
            Recordings.COLUMN_NAME_SOURCE + " TEXT NOT NULL DEFAULT 'unknown')";

    private static final String SQL_CREATE_LISTENED = "CREATE TABLE " + Contacts.TABLE_NAME +
            " (" + Contacts._ID + " INTEGER NOT NULL PRIMARY KEY, " +
            Contacts.COLUMN_NAME_NUMBER + " TEXT, " +
            Contacts.COLUMN_NAME_CONTACT_NAME + " TEXT, " +
            Contacts.COLUMN_NAME_PHOTO_URI + " TEXT, " +
            Contacts.COLUMN_NAME_PHONE_TYPE + " INTEGER NOT NULL, " +
            Contacts.COLUMN_NAME_PRIVATE_NUMBER + " INTEGER NOT NULL DEFAULT 0, " +
            "CONSTRAINT no_duplicates UNIQUE(" + Contacts.COLUMN_NAME_NUMBER + ") )";


    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "callrecorder.db";

    public CallRecorderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RECORDINGS);
        db.execSQL(SQL_CREATE_LISTENED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE " + Recordings.TABLE_NAME + " ADD COLUMN " + Recordings.COLUMN_NAME_SOURCE +
                " TEXT NOT NULL DEFAULT 'unknown'");
    }
}
