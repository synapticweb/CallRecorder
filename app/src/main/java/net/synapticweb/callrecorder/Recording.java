package net.synapticweb.callrecorder;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import net.synapticweb.callrecorder.databases.RecordingsContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;

import java.io.File;

public class Recording {
    private Context context;
    private long id;
    private String path;
    private static String TAG = "CallRecorder";

    Recording(Context context, long id, String path)
    {
        this.context = context;
        this.id = id;
        this.path = path;
    }

    public void delete() throws SQLException, SecurityException
    {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if((db.delete(RecordingsContract.Recordings.TABLE_NAME,
                RecordingsContract.Recordings._ID + "=" + getId(), null)) == 0)
            throw new SQLException("The Recording row was not deleted");

        File file = new File(path);
        file.delete();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


}
