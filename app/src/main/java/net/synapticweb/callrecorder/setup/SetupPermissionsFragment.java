/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.setup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;


public class SetupPermissionsFragment extends Fragment {
    private final static int PERMISSION_REQUEST = 0;
    private SetupActivity parentActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setup_permissions_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Resources res = getResources();
        parentActivity = (SetupActivity) getActivity();
        TextView permsIntro = parentActivity.findViewById(R.id.perms_intro);
        permsIntro.setText(String.format(res.getString(R.string.perms_intro), res.getString(R.string.app_name)));
                Button nextButton = parentActivity.findViewById(R.id.setup_perms_next);
        //în Android 6 dacă o singură permisiune este revocată le cere din nou pe toate. Nu pare suficient de sever pentru reparație.
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean notGranted = false;
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length == 0)
                notGranted = true;
            else {
                for (int result : grantResults)
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        notGranted = true;
                        break;
                    }
            }
            if (notGranted) {
                new MaterialDialog.Builder(parentActivity)
                        .title(R.string.warning_title)
                        .content(R.string.permissions_not_granted)
                        .positiveText(android.R.string.ok)
                        .icon(getResources().getDrawable(R.drawable.warning))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                permissionsNext();
                            }
                        })
                        .show();
            }
            else
                permissionsNext();
        }
    }

    private void permissionsNext() {
        int checkResult = parentActivity.getCheckResult();
        //după permisiuni afișăm powersetup dacă suntem la prima rulare sau aplicația este optimizată.
        if ((checkResult & ContactsListActivityMain.EULA_NOT_ACCEPTED) != 0 ||
                (checkResult & ContactsListActivityMain.POWER_OPTIMIZED) != 0) {
            SetupPowerFragment powerFragment = new SetupPowerFragment();
            parentActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.setup_fragment_container, powerFragment)
                    .commitAllowingStateLoss();
        }
        else
            parentActivity.finish();
    }
}
