package net.synapticweb.callrecorder.contactslist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import net.synapticweb.callrecorder.R;

public class UnassignedRecordingsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.unassigned_recordings_fragment, container, false);
    }
}
