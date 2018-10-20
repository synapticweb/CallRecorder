package net.synapticweb.callrecorder;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.contactdetail.ContactDetailActivity;
import net.synapticweb.callrecorder.data.Recording;

import java.lang.ref.WeakReference;

/*Am decis să folosesc AsyncTask pentru copierea recordingurilor, astfel încît threadul UI să rămînă liber - pentru a putea
   anula taskul dacă ar fi necesar. AS m-a obligat să fac clasa ExportAsyncTask statică - cică altfel colectorul de gunoaie
   nu poate acționa asupra ContactDetailActivity, fiindcă dacă ar fi nonstatică ar avea o referință permanentă la părinte.
   Pentru a implementa o bară de progres a fost necesar ca pusblishProgress() să fie apelată din Recording.export() și
   cîmpurile alreadyCopied și totalSize să fie accesibile din acea metodă. Pentru a realiza aceste lucruri am modificat export()
   astfel încît să primească drept parametru o referință la obiectul ExportAsyncTask (în Java obiectele sunt pasate în funcție
   ca referințe, nu ca valori - deci nu a trebuit să fac nimic suplimentar).
   Algoritmul de copiere l-am adaptat după ce am găsit aici: https://stackoverflow.com/questions/21239223/track-progress-of-copying-files
   (al doilea răspuns). În export() nu am putut apela direct publishProgress (pentru că e protected) așa că am folosit
   callPublishProgress.
   Am folosit MaterialDialog, care are o implementare frumoasă de progress dialog. Constructorul dialogului mi-a cerut o
   referință la activitate. N-am putut pune o referință în ExportAsyncTask - pentru că e statică, așa că am folosit metoda
   excelentă WeakReference. A se vedea și https://stackoverflow.com/questions/44309241/warning-this-asynctask-class-should-be-static-or-leaks-might-occur
   Dialogul l-am făcut modal (dialog.setCancelable(false)), pentru că altfel userul îl poate ascunde, dar taskul continuă în background ceea ce încetinește
   aplicația. Singura modalitate de a ascunde dialogul este butonul Cancel, care anulează taskul. De reținut faptul că apelarea
   cancel(true) nu încheie taskul - doar dă undă verde unei eventuale încheieri. Ca să se încheie este necesar ca doInBackground
   să verifice dacă s-a dat undă verde cu isCancelled și dacă da, să termine. Eu fac verificarea atît în export() cît și
   în doInBackground pentru că: dacă aș verifica numai în doInBackground terminarea s-ar produce numai după încheierea exportului
   unui fișier, și pot fi mari; dacă aș verifica numai în export() aș întrerupe exportul fișierului curent, dar doInBackground
   ar apela din nou export() și s-ar mai copia cîte 1MB din fiecare fișier rămas.
   */
public class ExportAsyncTask extends AsyncTask<Recording, Integer, Boolean> {
    public long alreadyCopied = 0;
    private String path;
    private long totalSize;
    private String phoneNumber;
    private MaterialDialog dialog;
    private WeakReference<Activity> activityRef; //http://sohailaziz05.blogspot.com/2014/10/asynctask-and-context-leaking.html
    private static final String TAG = "CallRecorder";

    public ExportAsyncTask(String foderPath, long totalSize, String phoneNumber, Activity activity) {
        this.path = foderPath;
        this.totalSize = totalSize;
        this.phoneNumber = phoneNumber;
        activityRef = new WeakReference<>(activity);
    }

    public void callPublishProgress(int progress) {
        publishProgress(progress);
    }

    @Override
    protected void onPreExecute() {
        dialog = new MaterialDialog.Builder(activityRef.get())
                .title("Progress")
                .content("Exporting recordings...")
                .progress(false, 100, true)
                .negativeText("Cancel")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        cancel(true);
                    }
                })
                .build();
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer...integers) {
        dialog.setProgress(integers[0]);
    }

    @Override
    protected void onCancelled() {
        new MaterialDialog.Builder(activityRef.get())
                .title("Warning")
                .content("The export was canceled. Some files might be corrupted or missing.")
                .positiveText("OK")
                .icon(activityRef.get().getResources().getDrawable(R.drawable.warning))
                .show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dialog.dismiss();
        if(result) {
            new MaterialDialog.Builder(activityRef.get())
                    .title("Success")
                    .content("The recording(s) were successfully exported.")
                    .positiveText("OK")
                    .icon(activityRef.get().getResources().getDrawable(R.drawable.success))
                    .show();
        }
        else {
            new MaterialDialog.Builder(activityRef.get())
                    .title("Error")
                    .content("An error occurred while exporting the recording(s). Some files might be corrupted or missing.")
                    .positiveText("OK")
                    .icon(activityRef.get().getResources().getDrawable(R.drawable.error))
                    .show();
        }
    }

    @Override
    protected Boolean doInBackground(Recording...recordings) {
        for(Recording recording : recordings) {
            try {
                recording.export(path, this, totalSize, phoneNumber);
                if(isCancelled())
                    break;
            }
            catch (Exception exc) {
                Log.wtf(TAG, exc.getMessage());
                return false;
            }
        }
        return true;
    }
}