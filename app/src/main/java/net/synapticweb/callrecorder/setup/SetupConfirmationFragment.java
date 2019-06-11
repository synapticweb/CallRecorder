package net.synapticweb.callrecorder.setup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;

import static android.app.Activity.RESULT_OK;

public class SetupConfirmationFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setup_confirmation_fragment, container, false);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SetupActivity parentActivity = (SetupActivity) getActivity();
        final int checkResult = parentActivity.getCheckResult();

        Button cancelButton = parentActivity.findViewById(R.id.setup_confirm_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra(SetupActivity.EXIT_APP, true);
                parentActivity.setResult(RESULT_OK, intent);
                parentActivity.finish();
            }
        });

        Button nextButton = parentActivity.findViewById(R.id.setup_confirm_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(parentActivity);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(ContactsListActivityMain.HAS_RUN_ONCE, true);
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
