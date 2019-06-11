package net.synapticweb.callrecorder.setup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;

public class SetupPowerFragment extends Fragment{
    private SetupActivity parentActivity;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setup_power_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //necesar pentru a ascunde controlul doze după ce a fost dezactivată optimizarea de către user.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) parentActivity.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(parentActivity.getPackageName())) {
                LinearLayout dozeInfo = parentActivity.findViewById(R.id.doze_info);
                dozeInfo.setVisibility(View.GONE);
            }
        }
    }

    @Override
    @SuppressLint("NewApi") //oricum este apelat doar de la 6 în sus.
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        parentActivity = (SetupActivity) getActivity();
        LinearLayout dozeInfo = parentActivity.findViewById(R.id.doze_info);
        if((parentActivity.getCheckResult() & ContactsListActivityMain.POWER_OPTIMIZED) != 0)
            dozeInfo.setVisibility(View.VISIBLE);
        Button turnOffDoze = parentActivity.findViewById(R.id.turn_off_doze);

        turnOffDoze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            }
        });

        Button finish = parentActivity.findViewById(R.id.setup_power_finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentActivity.finish();
            }
        });
    }
}
