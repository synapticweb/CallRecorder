package net.synapticweb.callrecorder.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.contactslist.ContactsListActivityMain;

public class SetupActivity extends TemplateActivity {
    private int checkResult;
    public static final String EXIT_APP = "exit_app";

    @Override
    protected Fragment createFragment() {
        if((checkResult & ContactsListActivityMain.EULA_NOT_ACCEPTED) != 0)
            return new SetupEulaFragment();
        else if((checkResult & ContactsListActivityMain.PERMS_NOT_GRANTED) != 0)
            return new SetupPermissionsFragment();
        else if((checkResult & ContactsListActivityMain.POWER_OPTIMIZED) != 0)
            return new SetupPowerFragment();
        else
            return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_activity);
        checkResult = getIntent().getIntExtra(ContactsListActivityMain.SETUP_ARGUMENT,
                ContactsListActivityMain.EULA_NOT_ACCEPTED & ContactsListActivityMain.PERMS_NOT_GRANTED &
                        ContactsListActivityMain.POWER_OPTIMIZED);
        insertFragment(R.id.setup_fragment_container);
    }

    public int getCheckResult() {
        return checkResult;
    }

    public void cancelSetup() {
        Intent intent = new Intent();
        intent.putExtra(SetupActivity.EXIT_APP, true);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        cancelSetup();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
