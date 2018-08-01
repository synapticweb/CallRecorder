package net.synapticweb.callrecorder;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import net.synapticweb.callrecorder.databases.RecordingsContract;
import net.synapticweb.callrecorder.databases.RecordingsDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Recording {
    private long id;
    private String path;
    private Boolean incoming;
    private Long startTimestamp, endTimestamp;

    Recording(long id, String path, Boolean incoming, Long startTimestamp, Long endTimestamp) {
        this.id = id;
        this.path = path;
        this.incoming = incoming;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    public String getDuration() {
        String date = "";
        long hours, minutes, seconds, remaining;
        long durationSeconds =  Math.round((endTimestamp - startTimestamp) / 1000);

        hours = (long) Math.floor(durationSeconds / 3600);
        remaining = durationSeconds % 3600;
        minutes = (long) Math.floor(remaining / 60);
        seconds = remaining % 60;

        if(hours > 0)
            date = hours + ":";
        if(hours > 0 && minutes < 10)
            date += "0" + minutes + ":";
        else
            date += minutes + ":";
        if(seconds < 10)
            date += "0" + seconds;
        else
            date += seconds;

        return date;
    }

    public String getDate() {
        return new SimpleDateFormat("d MMM yyyy - HH:mm:ss", Locale.US).format(new Date(startTimestamp));
    }

    public void delete(Context context) throws SQLException, SecurityException
    {
        RecordingsDbHelper mDbHelper = new RecordingsDbHelper(context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        if((db.delete(RecordingsContract.Recordings.TABLE_NAME,
                RecordingsContract.Recordings._ID + "=" + getId(), null)) == 0)
            throw new SQLException("The Recording row was not deleted");

        new File(path).delete();
    }

    public void export(String folderPath, PhoneNumberDetail.ExportAsyncTask asyncTask, long totalSize) throws IOException {
        String fileName = new SimpleDateFormat("d_MMM_yyyy_HH_mm_ss", Locale.US).format(new Date(startTimestamp)) + ".amr";
        InputStream in = new FileInputStream(path);
        OutputStream out = new FileOutputStream(new File(folderPath, fileName));

        byte[] buffer = new byte[1048576]; //dacă folosesc 1024 merge foarte încet
        int read;
        while ((read = in.read(buffer)) != -1) {
            asyncTask.alreadyCopied += read;
            out.write(buffer, 0, read);
            asyncTask.callPublishProgress(Math.round(100 * asyncTask.alreadyCopied / totalSize));
            if(asyncTask.isCancelled())
                break;
        }
        in.close();
        out.flush();
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

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

}
