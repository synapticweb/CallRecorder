/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;


import android.app.Activity;
import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;
import net.synapticweb.callrecorder.CrApp.DialogInfo;
import net.synapticweb.callrecorder.data.Repository;
import net.synapticweb.callrecorder.di.FragmentScope;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

@FragmentScope
public class ContactDetailPresenter implements ContactDetailContract.Presenter {
    private ContactDetailContract.View view;
    private Repository repository;

     @Inject
     ContactDetailPresenter(ContactDetailContract.View view, Repository repository) {
        this.view = view;
        this.repository = repository;
    }

    @Override
    public DialogInfo deleteContact(Contact contact) {
         try {
             contact.delete(repository, view.getContext());
         }
         catch (Exception exc) {
             CrLog.log(CrLog.ERROR, "Error deleting the contact: " + exc.getMessage());
             return new DialogInfo(R.string.error_title, R.string.error_deleting_contact, R.drawable.error);
         }
         return null;
    }

    @Override
    public DialogInfo renameRecording(CharSequence input, Recording recording) {
        if(Recording.hasIllegalChar(input))
            return new DialogInfo(R.string.information_title, R.string.rename_illegal_chars, R.drawable.info);

        String parent = new File(recording.getPath()).getParent();
        String oldFileName = new File(recording.getPath()).getName();
        String ext = oldFileName.substring(oldFileName.length() - 3);
        String newFileName = input + "." + ext;

        if(new File(parent, newFileName).exists())
            return new DialogInfo(R.string.information_title, R.string.rename_already_used, R.drawable.info);

        try {
            if(new File(recording.getPath()).renameTo(new File(parent, newFileName)) ) {
                recording.setPath(new File(parent, newFileName).getAbsolutePath());
                recording.setIsNameSet(true);
                recording.update(repository);
            }
            else
                throw new Exception("File.renameTo() has returned false.");
        }
        catch (Exception e) {
            CrLog.log(CrLog.ERROR, "Error renaming the recording:" + e.getMessage());
            return new DialogInfo(R.string.error_title, R.string.rename_error, R.drawable.error);
        }

        return null;
    }

    @Override
    public void loadRecordings(Contact contact) {
       repository.getRecordings(contact,  recordings -> {
                view.paintViews(recordings);
                //aici ar trebui să fie cod care să pună tickuri pe recordingurile selectate cînd
                //este întors device-ul. Dar dacă pun aici codul nu se execută pentru că nu vor fi gata
                //cardview-urile. Așa că acest cod se duce în RecordingAdapter::onBindViewHolder()
                // total - neintuitiv.
            });
    }

    @Override
    public DialogInfo deleteRecordings(List<Recording> recordings) {
        for(Recording recording :  recordings) {
            try {
                recording.delete(repository);
                view.removeRecording(recording);
            }
            catch (Exception exc) {
                CrLog.log(CrLog.ERROR, "Error deleting the selected recording(s): " + exc.getMessage());
                return new DialogInfo(R.string.error_title, R.string.error_deleting_recordings, R.drawable.error);
            }
        }
        return null;
    }

    @Override
    public void toggleShouldRecord(Contact contact) {
        contact.setShouldRecord(!contact.getShouldRecord());
        contact.update(repository);
        view.setContact(contact);
    }


    @Override
    public void moveSelectedRecordings(String path, int totalSize, Activity parentActivity, Recording[] recordings) {
        new MoveAsyncTask(repository, path, totalSize, parentActivity).
                execute(recordings);
    }
}
