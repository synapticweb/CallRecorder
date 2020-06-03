/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.setup;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import net.synapticweb.callrecorder.HelpActivity;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.Util;

public class ShowEulaActivity extends BaseActivity {
    @Override
    protected Fragment createFragment() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_show_eula_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_show_eula);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        String html = Util.rawHtmlToString(R.raw.eula);
        html = html.replace(HelpActivity.APP_NAME_PLACEHOLDER, getResources().getString(R.string.app_name));

        WebView eulaHtml = findViewById(R.id.eula_hmtl);
        eulaHtml.loadDataWithBaseURL("file:///android_asset/",
                html, "text/html", null, null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home)
            finish();
        return true;
    }
}
