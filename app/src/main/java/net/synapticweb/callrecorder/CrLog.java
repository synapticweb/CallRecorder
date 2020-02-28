/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.StringDef;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CrLog {
    private static final String TAG = "CallRecorder";
    private final static int MAX_FILE_SIZE = 1000000;
    private final static int MAX_FILE_COUNT = 5;
    private final static String LOG_FILE_NAME = "log";
    private final static File LOG_FOLDER = CrApp.getInstance().getFilesDir();

    public static final String DEBUG = " DEBUG ";
    public static final String WARN = " WARN ";
    public static final String ERROR = " ERROR ";
    @StringDef({DEBUG, WARN, ERROR})
    @Retention(RetentionPolicy.SOURCE)
    @interface levels {}

    private static File logFile = new File(LOG_FOLDER, LOG_FILE_NAME + ".txt");


    private static void backupLogFiles() throws LoggerException {
        File backup = null;
        for(int i = 1; i <= MAX_FILE_COUNT; ++i) {
            backup = new File(LOG_FOLDER, LOG_FILE_NAME + i + ".txt");
            if(!backup.exists()) {
                backup = new File(LOG_FOLDER, LOG_FILE_NAME + i + ".txt");
                break;
            }
            if(i == MAX_FILE_COUNT) {
                File firstBackup = new File(LOG_FOLDER, LOG_FILE_NAME + "1.txt");
                if(!firstBackup.delete())
                    throw new LoggerException("Cannot delete last backup");

                for(int j = 2; j <= MAX_FILE_COUNT; ++j) {
                    File currentBackup = new File(LOG_FOLDER, LOG_FILE_NAME + j + ".txt");
                    if(!currentBackup.renameTo(new File(LOG_FOLDER, LOG_FILE_NAME + (j - 1) + ".txt")))
                        throw new LoggerException("Could not rename backup file " + currentBackup.getName());
                }
                backup = new File(LOG_FOLDER, LOG_FILE_NAME + MAX_FILE_COUNT + ".txt");
            }
        }

        if(!logFile.renameTo(backup))
            throw new LoggerException("Could not rename log file");
    }

    private static void writeHeader() throws IOException {
        String header = "";
        header += "APP VERSION CODE: " + BuildConfig.VERSION_CODE + "\n";
        header += "APP VERSION: " + BuildConfig.VERSION_NAME + "\n";
        header += "MODEL: " + Build.MODEL + "\n";
        header += "MANUFACTURER: " + Build.MANUFACTURER + "\n";
        header += "SDK: " + Build.VERSION.SDK_INT + "\n";
        header += "BOARD: " + Build.BOARD + "\n";
        header += "BRAND: " + Build.BRAND + "\n";
        header += "DEVICE: " + Build.DEVICE + "\n";
        header += "DISPLAY NAME: " + Build.DISPLAY + "\n";
        header += "HARDWARE: " + Build.HARDWARE + "\n";
        header += "PRODUCT: " + Build.PRODUCT + "\n";
        header += "WIDTH PIXELS: " + CrApp.getInstance().getResources().getDisplayMetrics().widthPixels + "\n";
        header += "HEIGHT PIXELS: " + CrApp.getInstance().getResources().getDisplayMetrics().heightPixels + "\n";
        header += "DENSITY PIXELS: " + CrApp.getInstance().getResources().getDisplayMetrics().density + "\n\n";

        BufferedWriter buffer = new BufferedWriter(new FileWriter(logFile, true));
        buffer.append(header);
        buffer.newLine();
        buffer.close();
    }

    public static void log(@levels String level, String message) {
        if(BuildConfig.DEBUG) {
            switch (level) {
                case DEBUG:
                    Log.d(TAG, message);
                    break;
                case WARN:
                    Log.w(TAG, message);
                    break;
                case ERROR:
                    Log.wtf(TAG, message);
            }
        }

        if(!logFile.exists()) {
            try {
               if(!logFile.createNewFile())
                   throw new LoggerException("Cannot create log file.");
               writeHeader();
            }
            catch (LoggerException | IOException e){
                Log.wtf(TAG, e.getMessage());
                return ;
            }
        }
        //check the file size
        if(logFile.length() > MAX_FILE_SIZE) {
            try {
                backupLogFiles();
                if (!logFile.createNewFile())
                    throw new LoggerException("Cannot create log file.");
                writeHeader();
            } catch (IOException | LoggerException e) {
                Log.wtf(TAG, e.getMessage());
                return ;
            }
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.US).format(
                new Date(System.currentTimeMillis()));

        try {
            BufferedWriter buffer = new BufferedWriter(new FileWriter(logFile, true));
            buffer.append(timestamp)
                    .append(level)
                    .append(message);
            buffer.newLine();
            buffer.close();
        }
        catch (IOException e) {
            Log.wtf(TAG, e.getMessage());
        }
    }

    static class SendLogs implements Runnable {
        AfterZip afterZip;

        SendLogs(AfterZip afterZip) {
            this.afterZip = afterZip;
        }

        @Override
        public void run() {
            Context context = CrApp.getInstance();
            final int BUFFER_SIZE = 2048;

            List<File> files = new ArrayList<>();
            if(logFile.exists())
                files.add(logFile);
            for(int i = 1; i <= MAX_FILE_COUNT; ++i){
                File file = new File(LOG_FOLDER, LOG_FILE_NAME + i + ".txt");
                if(file.exists())
                    files.add(file);
                else
                    break;
            }

            if(files.isEmpty() || context.getExternalFilesDir(null) == null)  //dacă external storage is unavailable.
                // În cazul acesta nu avem cum pune arhiva într-o locație accesibilă clienților de mail și ieșim.
                return ;

            File zipFile = new File(context.getExternalFilesDir(null), "logs.zip");
            if(zipFile.exists())
                if(!zipFile.delete())
                    Log.wtf(TAG, "Cannot delete old zip file.");

            byte [] data = new byte[BUFFER_SIZE];
            try {
                ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

                for (File file : files) {
                    BufferedInputStream input = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
                    ZipEntry entry = new ZipEntry(file.getName());
                    zout.putNextEntry(entry);
                    int count;

                    while((count = input.read(data, 0, BUFFER_SIZE)) != -1)
                        zout.write(data, 0, count);

                    input.close();
                }
                zout.finish();
                zout.close();
            }
            catch (IOException e) {
                Log.wtf(TAG, e.getMessage());
                return ;
            }

            afterZip.doTheRest(zipFile);
        }

        interface AfterZip {
            void doTheRest(File zipFile);
        }
    }

    static public void sendLogs(final AppCompatActivity activity) {
        new Thread(new SendLogs(new SendLogs.AfterZip() {
            @Override
            public void doTheRest(File zipFile) {
                Resources res = CrApp.getInstance().getResources();
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + res.getString(R.string.dev_email)));
                intent.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.app_name) + " logs");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile));
                activity.startActivity(Intent.createChooser(intent, "Send email..."));
            }
        })).start();
    }

    static class LoggerException extends Exception {
        LoggerException(String message) {
            super(message);
        }
    }
}
