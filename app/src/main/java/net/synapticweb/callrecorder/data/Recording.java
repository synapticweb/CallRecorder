/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.data;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactdetail.MoveAsyncTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Recording implements Parcelable {
    private Long id = 0L;
    private Long contactId;
    private String path;
    private Boolean incoming;
    private Long startTimestamp, endTimestamp;
    private Boolean isNameSet;
    private String format;
    private String mode;
    private String source;

    public Recording() {}

    public Recording(Long id, Long contactId, String path, Boolean incoming, Long startTimestamp, Long endTimestamp,
                     String format, Boolean isNameSet, String mode, String source) {
        if(id != null) this.id = id;
        if(contactId != null) this.contactId = contactId;
        if(path != null) this.path = path;
        if(incoming != null) this.incoming = incoming;
        if(startTimestamp != null) this.startTimestamp = startTimestamp;
        if(endTimestamp != null) this.endTimestamp = endTimestamp;
        if(isNameSet != null) this.isNameSet = isNameSet;
        if(format != null) this.format = format;
        if(mode != null) this.mode = mode;
        if(source != null) this.source = source;
    }

    public boolean exists() {
        return new File(path).isFile();
    }

    public boolean isSavedInPrivateSpace(Context context) {
        return new File(path).getParentFile().
                compareTo(context.getFilesDir()) == 0;
    }

    public long getLength() {
        return endTimestamp - startTimestamp;
    }

    public void update(Repository repository){
        repository.updateRecording(this);
    }

    public void save(Repository repository) {
        repository.insertRecording(this);
    }

    public String getName() {
        if(!isNameSet)
            return getDate() + " " + getTime();
        String fileName = new File(path).getName();
        return fileName.substring(0, fileName.length() - 4);
    }

    public static boolean hasIllegalChar(CharSequence fileName) {
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9.\\- ]");
        Matcher matcher = pattern.matcher(fileName);
        return matcher.find();
    }

    public long getSize() {
        return new File(path).length();
    }


    public String getDate() {
        Calendar recordingCal = Calendar.getInstance();
        recordingCal.setTimeInMillis(startTimestamp);
        return new SimpleDateFormat("d MMMM yyyy", Locale.US).format(new Date(startTimestamp));
    }

    public String getTime() {
        return new SimpleDateFormat("h:mm a", Locale.US).format(new Date(startTimestamp)); //3:45 PM
    }

    public void delete(Repository repository) throws SecurityException {
        repository.deleteRecording(this);
        new File(path).delete();
    }

    public void move(Repository repository, String folderPath, @Nullable MoveAsyncTask asyncTask, long totalSize)
            throws IOException {
        String fileName = new File(path).getName();
        InputStream in = new FileInputStream(path);
        OutputStream out = new FileOutputStream(new File(folderPath, fileName));

        byte[] buffer = new byte[1048576]; //dacă folosesc 1024 merge foarte încet
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if(asyncTask != null) {
                asyncTask.alreadyCopied += read;
                asyncTask.callPublishProgress(Math.round(100 * asyncTask.alreadyCopied / totalSize));
                if (asyncTask.isCancelled())
                    break;
            }
        }
        in.close();
        out.flush();
        new File(path).delete();
        path = new File(folderPath, fileName).getAbsolutePath();
        repository.updateRecording(this);
    }

    public String getHumanReadingFormat(Context context) {
        final int wavBitrate = 705, aacHighBitrate = 128, aacMedBitrate = 64, aacBasBitrate = 32;
        Resources res = context.getResources();
        switch (format) {
            case "wav":
                return res.getString(R.string.lossless_quality) + " (WAV), 44khz 16bit WAV " + (mode.equals("mono") ? wavBitrate : wavBitrate * 2)
                        + "kbps " + mode.substring(0, 1).toUpperCase() + mode.substring(1);
            case "aac_hi":
                return res.getString(R.string.hi_quality) + " (AAC), 44khz 16bit AAC128 " + (mode.equals("mono") ? aacHighBitrate : aacHighBitrate * 2)
                        + "kbps " + mode.substring(0, 1).toUpperCase() + mode.substring(1);
            case "aac_med":
                return res.getString(R.string.med_quality) + " (AAC), 44khz 16bit AAC64 " + (mode.equals("mono") ? aacMedBitrate : aacMedBitrate * 2)
                        + "kbps " + mode.substring(0, 1).toUpperCase() + mode.substring(1);
            case "aac_bas":
                return res.getString(R.string.bas_quality) + " (AAC), 44khz 16bit AAC32 " + (mode.equals("mono") ? aacBasBitrate : aacBasBitrate * 2)
                        + "kbps " + mode.substring(0, 1).toUpperCase() + mode.substring(1);
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setContactId(long contactId) { this.contactId = contactId; }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public Boolean getIsNameSet() { return isNameSet; }

    public void setIsNameSet(Boolean isNameSet) { this.isNameSet = isNameSet; }

    public Long getContactId() { return contactId; }

    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getSource() { return source; }

    public void setSource(String source) { this.source = source; }

    public Long getStartTimestamp() {
        return startTimestamp;
    }

    public Long getEndTimestamp() {
        return endTimestamp;
    }

    public String getMode() {
        return mode;
    }

    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public void setEndTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    /** Necesară pentru comparațiile din teste. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recording recording = (Recording) o;
        return id.equals(recording.id) &&
                Objects.equals(contactId, recording.contactId) &&
                Objects.equals(path, recording.path) &&
                Objects.equals(incoming, recording.incoming) &&
                Objects.equals(startTimestamp, recording.startTimestamp) &&
                Objects.equals(endTimestamp, recording.endTimestamp) &&
                Objects.equals(isNameSet, recording.isNameSet) &&
                Objects.equals(format, recording.format) &&
                Objects.equals(mode, recording.mode) &&
                Objects.equals(source, recording.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.id);
        dest.writeValue(this.contactId);
        dest.writeString(this.path);
        dest.writeValue(this.incoming);
        dest.writeValue(this.startTimestamp);
        dest.writeValue(this.endTimestamp);
        dest.writeValue(this.isNameSet);
        dest.writeString(this.format);
        dest.writeString(this.mode);
        dest.writeString(this.source);
    }

    protected Recording(Parcel in) {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.contactId = (Long) in.readValue(Long.class.getClassLoader());
        this.path = in.readString();
        this.incoming = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.startTimestamp = (Long) in.readValue(Long.class.getClassLoader());
        this.endTimestamp = (Long) in.readValue(Long.class.getClassLoader());
        this.isNameSet = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.format = in.readString();
        this.mode = in.readString();
        this.source = in.readString();
    }

    public static final Creator<Recording> CREATOR = new Creator<Recording>() {
        @Override
        public Recording createFromParcel(Parcel source) {
            return new Recording(source);
        }

        @Override
        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };
}
