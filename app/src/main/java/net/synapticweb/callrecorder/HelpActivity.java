/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.tabs.TabLayout;

public class HelpActivity extends BaseActivity {
    ViewPager pager;
    HelpPagerAdapter adapter;
    static final int NUM_PAGES = 6;
    public static final String APP_NAME_PLACEHOLDER = "APP_NAME";
    static String[] content = new String[NUM_PAGES];
    static String[] contentTitles = new String[NUM_PAGES];

    //am folosit R.raw pentru posibilitatea traducerii: res/raw-de/ for german

    @Override
    protected Fragment createFragment() { return null; }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        Resources res = getResources();

        content[0] = Util.rawHtmlToString(R.raw.help_recording_calls);
        content[1] = Util.rawHtmlToString(R.raw.help_playing_recordings);
        content[2] = Util.rawHtmlToString(R.raw.help_managing_recordings);
        content[3] = Util.rawHtmlToString(R.raw.help_about);
        content[3] = String.format(content[3], BuildConfig.VERSION_NAME,
                res.getString(R.string.dev_email), res.getString(R.string.dev_email),
                res.getString(R.string.send_devs));
        content[4] = Util.rawHtmlToString(R.raw.eula);
        content[5] = Util.rawHtmlToString(R.raw.help_licences);

        for(int i = 0; i < content.length; ++i)
            content[i] = content[i].replace(APP_NAME_PLACEHOLDER, res.getString(R.string.app_name));

        if(getSettedTheme().equals(BaseActivity.DARK_THEME)) {
            for(int i = 0; i < content.length; ++i)
                content[i] = content[i].replace("light", "dark");
        }

        contentTitles[0] = res.getString(R.string.help_title2);
        contentTitles[1] = res.getString(R.string.help_title3);
        contentTitles[2] = res.getString(R.string.help_title4);
        contentTitles[3] = res.getString(R.string.about_name);
        contentTitles[4] = res.getString(R.string.help_title5);
        contentTitles[5] = res.getString(R.string.help_title7);

        setContentView(R.layout.help_activity);
        pager = findViewById(R.id.help_pager);
        adapter = new HelpPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        TabLayout tabLayout = findViewById(R.id.help_tab_layout);
        tabLayout.setupWithViewPager(pager);

        Toolbar toolbar = findViewById(R.id.toolbar_help);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    static class HelpPagerAdapter extends FragmentPagerAdapter {
        HelpPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return HelpFragment.newInstance(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return contentTitles[position];
        }
    }

     static public class HelpFragment extends Fragment {
        static final String ARG_POSITION = "arg_pos";
        int position;

         static HelpFragment newInstance(int position) {
            HelpFragment fragment = new HelpFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_POSITION, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            position = getArguments() != null ? getArguments().getInt(ARG_POSITION) : 0;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.help_fragment, container, false);
            WebView htmlText = view.findViewById(R.id.help_fragment_text);
            htmlText.getSettings().setJavaScriptEnabled(true);
            htmlText.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public void sendLogs() {
                    new MaterialDialog.Builder(getActivity())
                            .content(R.string.send_devs_question)
                            .positiveText(android.R.string.ok)
                            .negativeText(android.R.string.cancel)
                            .onPositive((@NonNull MaterialDialog dialog, @NonNull DialogAction which) ->
                                    CrLog.sendLogs((AppCompatActivity) getActivity())
                            )
                            .show();

                }
            }, "SendLogsWrapper");
            //am pus imaginile și style-urile în main/assets. Ca urmare am setat base url la file:///android_asset/ și sursele
            //sunt doar numele fișierelor.
            htmlText.loadDataWithBaseURL("file:///android_asset/", content[position], "text/html", null, null);
            return view;
        }
    }
}
