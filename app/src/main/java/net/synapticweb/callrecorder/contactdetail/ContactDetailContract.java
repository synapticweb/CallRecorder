package net.synapticweb.callrecorder.contactdetail;

import android.os.Bundle;


import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public interface ContactDetailContract {
    interface View {
        void setContact(Contact contact);
        Contact getContact();
        void paintViews(List<Recording> recordings);
        void setSelectMode(boolean isSelectModeOn);
        boolean getSelectMode();
        void addToSelectedItems(int adapterPosition);
        boolean removeIfPresentInSelectedItems(int adapterPosition);
        void toggleSelectModeActionBar();
        void selectRecording(CardView card);
        void deselectRecording(CardView card);
        boolean isEmptySelectedItems();
        void clearSelectedMode();
        List<Recording> getSelectedRecordings();
        AppCompatActivity getParentActivity();
        void displayRecordingStatus();
        boolean isSinglePaneLayout();
        void setActionBarTitleIfActivityDetail();
        RecyclerView getRecordingsRecycler();
        ContactDetailFragment.RecordingAdapter getRecordingsAdapter();
        List<Integer> getSelectedItems();
    }

    interface ContactDetailPresenter {
        void deleteContact(final Contact contact);
        void editContact(final Contact contact);
        void onEditActivityResult(Bundle result);
        void loadRecordings(Contact contact);
        void selectRecording(CardView card, int adapterPosition);
        void startPlayerActivity(Recording recording);
        void deleteSelectedRecordings();
        void exportSelectedRecordings(String path);
        void toggleShouldRecord(Contact contact);
        void callContact(Contact contact);
        void toggleSelectAll();
    }
}
