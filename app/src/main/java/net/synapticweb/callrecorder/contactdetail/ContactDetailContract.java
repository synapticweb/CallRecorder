package net.synapticweb.callrecorder.contactdetail;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;

import net.synapticweb.callrecorder.data.Contact;
import net.synapticweb.callrecorder.data.Recording;

import java.util.List;

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
        void toggleSelectedRecording(CardView card);
        boolean isEmptySelectedItems();
        void clearSelectedMode();
        List<Recording> getSelectedRecordings();
        AppCompatActivity getParentActivity();
        void displayRecordingStatus();
        boolean isSinglePaneLayout();
        void setActionBarTitleIfActivityDetail();
        void toggleSelectedMultipleRecordings();
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
