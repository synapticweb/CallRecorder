/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.setup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;

import java.util.Objects;

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
    @SuppressLint("NewApi") //pentru a suprima avertismentul aferent pornirii activității de oprire a optimizării.
    //Dar turnOffDoze primește clicklistener doar dacă android >= 6.
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        parentActivity = (SetupActivity) getActivity();
        Resources res = getResources();
        TextView dozeInfoText = parentActivity.findViewById(R.id.doze_info_text);
        dozeInfoText.setText(String.format(res.getString(R.string.doze_info), res.getString(R.string.app_name)));
        TextView otherOptimizations = parentActivity.findViewById(R.id.other_power_optimizations);
        otherOptimizations.setText(String.format(res.getString(R.string.other_power_optimizations), res.getString(R.string.app_name)));

        LinearLayout dozeInfo = parentActivity.findViewById(R.id.doze_info);
        if((parentActivity.getCheckResult() & ContactsListActivityMain.POWER_OPTIMIZED) != 0) {
            dozeInfo.setVisibility(View.VISIBLE);
            Button turnOffDoze = parentActivity.findViewById(R.id.turn_off_doze);

            turnOffDoze.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //pentru a rezolva crash-ul e86a71db-dca0-4064-9bb5-466c6fd9dfce
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    PackageManager pm = requireActivity().getPackageManager();
                    if (intent.resolveActivity(pm) != null)
                        startActivity(intent);
                }
            });
        }

        Button finish = parentActivity.findViewById(R.id.setup_power_finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PowerManager pm = (PowerManager) parentActivity.getSystemService(Context.POWER_SERVICE);
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(parentActivity.getPackageName()))
                        new MaterialDialog.Builder(parentActivity)
                                .title(R.string.warning_title)
                                .content(R.string.optimization_still_active)
                                .positiveText(android.R.string.ok)
                                .icon(getResources().getDrawable(R.drawable.warning))
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        parentActivity.finish();
                                    }
                                })
                                .show();
                    else
                        parentActivity.finish();
                }
                else
                    parentActivity.finish();
            }
        });
    }
}
