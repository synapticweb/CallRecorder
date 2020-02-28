/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.contactdetail;

import android.os.Bundle;
import android.view.View;


import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public interface ContactDetailContract {
    interface View {
        void setContact(Contact contact);
        Contact getContact();
        void paintViews(List<Recording> recordings);
        void setSelectMode(boolean isSelectModeOn);
        boolean isSelectModeOn();
        void addToSelectedItems(int adapterPosition);
        boolean removeIfPresentInSelectedItems(int adapterPosition);
        void toggleSelectModeActionBar(boolean animateAlpha);
        void toggleSelectModeRecording(android.view.View recording, boolean animate);
        void selectRecording(android.view.View recording);
        void deselectRecording(android.view.View recording);
        boolean isEmptySelectedItems();
        void clearSelectedMode();
        List<Recording> getSelectedRecordings();
        AppCompatActivity getParentActivity();
        boolean isSinglePaneLayout();
        void setActionBarTitleIfActivityDetail();
        RecyclerView getRecordingsRecycler();
        ContactDetailFragment.RecordingAdapter getRecordingsAdapter();
        List<Integer> getSelectedItems();
        void updateTitle();
        void disableMoveBtn();
        void enableMoveBtn();
        int getSelectedItemsDeleted();
        void setSelectedItemsDeleted(int selectedItemsDeleted);
    }

    interface ContactDetailPresenter {
        void deleteContact(final Contact contact);
        void editContact(final Contact contact);
        void onEditActivityResult(Bundle result);
        void loadRecordings(Contact contact);
        void selectRecording(android.view.View recording, int adapterPosition, boolean exists);
        void startPlayerActivity(Recording recording);
        void deleteSelectedRecordings();
        void moveSelectedRecordings(String path);
        void callContact(Contact contact);
        void toggleSelectAll();
        void onInfoClick();
        void onRenameClick();
        void storageInfo();
    }
}
