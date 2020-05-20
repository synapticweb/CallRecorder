package net.synapticweb.callrecorder;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.VisibleForTesting;

import net.synapticweb.callrecorder.data.CallRecorderDbHelper;
import net.synapticweb.callrecorder.data.Repository;
import net.synapticweb.callrecorder.data.RepositoryImpl;

public class ServiceProvider {
    private static volatile Repository repository = null;

    @VisibleForTesting
    public static void setRepository(Repository repository) {
        ServiceProvider.repository = repository;
    }

    public static synchronized Repository provideRepository(Context context) {
            if (repository == null) {
                SQLiteOpenHelper helper = new CallRecorderDbHelper(context);
                repository = RepositoryImpl.getInstance(helper);
            }
            return repository;
        }
}
