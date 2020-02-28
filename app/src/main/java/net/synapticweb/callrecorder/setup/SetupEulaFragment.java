/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import net.synapticweb.callrecorder.BuildConfig;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;

public class SetupEulaFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setup_eula_fragment, container, false);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SetupActivity parentActivity = (SetupActivity) getActivity();
        final int checkResult = parentActivity.getCheckResult();

        TextView version = parentActivity.findViewById(R.id.app_version);
        version.setText(String.format(parentActivity.getResources().getString(R.string.version_eula_screen),
                BuildConfig.VERSION_NAME) );

        Button showEula = parentActivity.findViewById(R.id.show_eula);
        showEula.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), ShowEulaActivity.class));
            }
        });

        Button cancelButton = parentActivity.findViewById(R.id.setup_confirm_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentActivity.cancelSetup();
            }
        });

        Button nextButton = parentActivity.findViewById(R.id.setup_confirm_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox hasAccepted = parentActivity.findViewById(R.id.has_accepted);
                if(!hasAccepted.isChecked())
                    return ;

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(parentActivity);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(ContactsListActivityMain.HAS_ACCEPTED_EULA, true);
                editor.apply();

                if((checkResult & ContactsListActivityMain.PERMS_NOT_GRANTED) != 0) {
                    SetupPermissionsFragment permissionsFragment = new SetupPermissionsFragment();
                    parentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.setup_fragment_container, permissionsFragment)
                            .commitAllowingStateLoss();

                } //dacă suntem în această metodă înseamnă că ne aflăm la prima rulare. Deci, trebuie să
                // se afișeze cel puțin avertismentul "protected apps" sau și controllerul doze, dacă nu avem
                //treabă cu permisiunile.
                else {
                    SetupPowerFragment powerFragment = new SetupPowerFragment();
                    parentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.setup_fragment_container, powerFragment)
                            .commitAllowingStateLoss();
                }
            }
        });
    }
}
