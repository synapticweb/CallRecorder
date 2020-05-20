/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;

import android.app.Activity;
import android.os.AsyncTask;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.data.Repository;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

/*Am decis să folosesc AsyncTask pentru copierea recordingurilor, astfel încît threadul UI să rămînă liber - pentru a putea
   anula taskul dacă ar fi necesar. AS m-a obligat să fac clasa MoveAsyncTask statică - cică altfel colectorul de gunoaie
   nu poate acționa asupra ContactDetailActivity, fiindcă dacă ar fi nonstatică ar avea o referință permanentă la părinte.
   Pentru a implementa o bară de progres a fost necesar ca pusblishProgress() să fie apelată din Recording.export() și
   cîmpurile alreadyCopied și totalSize să fie accesibile din acea metodă. Pentru a realiza aceste lucruri am modificat export()
   astfel încît să primească drept parametru o referință la obiectul MoveAsyncTask (în Java obiectele sunt pasate în funcție
   ca referințe, nu ca valori - deci nu a trebuit să fac nimic suplimentar).
   Algoritmul de copiere l-am adaptat după ce am găsit aici: https://stackoverflow.com/questions/21239223/track-progress-of-copying-files
   (al doilea răspuns). În export() nu am putut apela direct publishProgress (pentru că e protected) așa că am folosit
   callPublishProgress.
   Am folosit MaterialDialog, care are o implementare frumoasă de progress dialog. Constructorul dialogului mi-a cerut o
   referință la activitate. N-am putut pune o referință în MoveAsyncTask - pentru că e statică, așa că am folosit metoda
   excelentă WeakReference. A se vedea și https://stackoverflow.com/questions/44309241/warning-this-asynctask-class-should-be-static-or-leaks-might-occur
   Dialogul l-am făcut modal (dialog.setCancelable(false)), pentru că altfel userul îl poate ascunde, dar taskul continuă în background ceea ce încetinește
   aplicația. Singura modalitate de a ascunde dialogul este butonul Cancel, care anulează taskul. De reținut faptul că apelarea
   cancel(true) nu încheie taskul - doar dă undă verde unei eventuale încheieri. Ca să se încheie este necesar ca doInBackground
   să verifice dacă s-a dat undă verde cu isCancelled și dacă da, să termine. Eu fac verificarea atît în export() cît și
   în doInBackground pentru că: dacă aș verifica numai în doInBackground terminarea s-ar produce numai după încheierea exportului
   unui fișier, și pot fi mari; dacă aș verifica numai în export() aș întrerupe exportul fișierului curent, dar doInBackground
   ar apela din nou export() și s-ar mai copia cîte 1MB din fiecare fișier rămas.
   */
public class MoveAsyncTask extends AsyncTask<Recording, Integer, Boolean> {
    public long alreadyCopied = 0;
    private String path;
    private long totalSize;
    private MaterialDialog dialog;
    private Repository repository;
    private WeakReference<Activity> activityRef; //http://sohailaziz05.blogspot.com/2014/10/asynctask-and-context-leaking.html

    MoveAsyncTask(Repository repository, String folderPath, long totalSize, Activity activity) {
        this.path = folderPath;
        this.totalSize = totalSize;
        this.repository = repository;
        activityRef = new WeakReference<>(activity);
    }

    public void callPublishProgress(int progress) {
        publishProgress(progress);
    }

    @Override
    protected void onPreExecute() {
        dialog = new MaterialDialog.Builder(activityRef.get())
                .title(R.string.progress_title)
                .content(R.string.progress_text)
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
                .title(R.string.warning_title)
                .content(R.string.canceled_move)
                .positiveText("OK")
                .icon(activityRef.get().getResources().getDrawable(R.drawable.warning))
                .show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dialog.dismiss();
        if(result) {
            new MaterialDialog.Builder(activityRef.get())
                    .title(R.string.success_move_title)
                    .content(R.string.success_move_text)
                    .positiveText("OK")
                    .icon(activityRef.get().getResources().getDrawable(R.drawable.success))
                    .show();
        }
        else {
            new MaterialDialog.Builder(activityRef.get())
                    .title(R.string.error_title)
                    .content(R.string.error_move)
                    .positiveText("OK")
                    .icon(activityRef.get().getResources().getDrawable(R.drawable.error))
                    .show();
        }
    }

    @Override
    protected Boolean doInBackground(Recording...recordings) {
        for(Recording recording : recordings) {
            try {
                recording.move(repository, path, this, totalSize);
                if(isCancelled())
                    break;
            }
            catch (Exception exc) {
                CrLog.log(CrLog.ERROR, "Error moving the recording(s): " + exc.getMessage());
                return false;
            }
        }
        return true;
    }
}