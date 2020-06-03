/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.player;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import net.synapticweb.callrecorder.CrLog;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.BaseActivity;
import net.synapticweb.callrecorder.Util;
import net.synapticweb.callrecorder.contactdetail.ContactDetailFragment;
import net.synapticweb.callrecorder.data.Recording;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.chibde.visualizer.LineBarVisualizer;
import com.sdsmdg.harjot.crollerTest.Croller;

public class PlayerActivity extends BaseActivity {
    AudioPlayer player;
    Recording recording;
    ImageButton playPause, resetPlaying;
    TextView recordingInfo;
    SeekBar playSeekBar;
    TextView playedTime, totalTime;
    boolean userIsSeeking = false;
    LineBarVisualizer visualizer;
    AudioManager audioManager;
    int phoneVolume;
    Croller gainControl, volumeControl;
    final static int AUDIO_SESSION_ID = 0;
    final static String IS_PLAYING = "is_playing";
    final static String CURRENT_POS = "current_pos";
    final static int DENSITY_PORTRAIT = 70;
    final static int DENSITY_LANDSCAPE = 150;

    public Fragment createFragment() {return null;}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setContentView(R.layout.player_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_player);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.player_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recording = getIntent().getParcelableExtra(ContactDetailFragment.RECORDING_EXTRA);
        visualizer = findViewById(R.id.visualizer);
        visualizer.setColor(getResources().getColor(R.color.colorAccentLighter));
        visualizer.setDensity(getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT ? DENSITY_PORTRAIT : DENSITY_LANDSCAPE);
        //crash report nr. 886:
        try {
            visualizer.setPlayer(AUDIO_SESSION_ID);
        }
        catch (Exception exc) {
            CrLog.log(CrLog.ERROR, "Error initializing visualizer.");
            visualizer = null;
        }
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        playPause = findViewById(R.id.test_player_play_pause);
        resetPlaying = findViewById(R.id.test_player_reset);
        playSeekBar = findViewById(R.id.play_seekbar);
        playedTime = findViewById(R.id.test_play_time_played);
        totalTime = findViewById(R.id.test_play_total_time);

        playPause.setOnClickListener((view) -> {
                if(player.getPlayerState() == PlayerAdapter.State.PLAYING) {
                    player.pause();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else if(player.getPlayerState() == PlayerAdapter.State.PAUSED ||
                        player.getPlayerState() == PlayerAdapter.State.INITIALIZED){
                    player.play();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
        });

        resetPlaying.setOnClickListener((view) -> {
                if(player.getPlayerState() == PlayerAdapter.State.PLAYING)
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                player.reset();
        });

        playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userSelectedPosition = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    userSelectedPosition = progress;
                playedTime.setText(Util.getDurationHuman(progress, false));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { userIsSeeking = true; }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userIsSeeking = false;
                player.seekTo(userSelectedPosition);
            }
        });

        gainControl = findViewById(R.id.gain_control);
        gainControl.setOnProgressChangedListener((progress) ->
                player.setGain((float) progress)
        );

        volumeControl = findViewById(R.id.volume_control);
        if(audioManager != null) {
            volumeControl.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            phoneVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeControl.setProgress(phoneVolume);
        }
        volumeControl.setOnProgressChangedListener( (progress) ->
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
        );

        recordingInfo = findViewById(R.id.recording_info);
        recordingInfo.setText(String.format(getResources().getString(R.string.recording_info),
                recording.getName(), recording.getHumanReadingFormat(getApplicationContext())));

//        Log.wtf(TAG, "Available width: " + getResources().getDisplayMetrics().widthPixels);
//        Log.wtf(TAG, "Density: " + getResources().getDisplayMetrics().density);
//        Log.wtf(TAG, "Density dpi: " + getResources().getDisplayMetrics().densityDpi);
//        Log.wtf(TAG, "Density scaled: " + getResources().getDisplayMetrics().scaledDensity);
    }

    //necesar pentru că dacă apăs pur și simplu pe săgeata back îmi apelează onCreate al activității contactdetail
    //fără un obiect Contact valid.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(visualizer != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                visualizer.setDensity(DENSITY_LANDSCAPE);
            else
                visualizer.setDensity(DENSITY_PORTRAIT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(player.getPlayerState() == PlayerAdapter.State.UNINITIALIZED ||
//                player.getPlayerState() == PlayerAdapter.State.STOPPED) {
        player = new AudioPlayer(new PlaybackListener());
        playedTime.setText("00:00");
        if(!player.loadMedia(recording.getPath()))
            return ;

        totalTime.setText(Util.getDurationHuman(player.getTotalDuration(), false));
        player.setGain(gainControl.getProgress());
//        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int currentPosition = pref.getInt(CURRENT_POS, 0);
        boolean isPlaying = pref.getBoolean(IS_PLAYING, true);
        if(!player.setMediaPosition(currentPosition)) {
            return ;
        }

        if(isPlaying) {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
            player.play();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
            player.setPlayerState(PlayerAdapter.State.PAUSED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(CURRENT_POS, player.getCurrentPosition());
        editor.putBoolean(IS_PLAYING, player.getPlayerState() == PlayerAdapter.State.PLAYING);
        editor.apply();
        player.stopPlayer();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //e necesar?
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(IS_PLAYING);
        editor.remove(CURRENT_POS);
        editor.apply();
        if(visualizer != null)
            visualizer.release();
        if(audioManager != null)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, phoneVolume, 0);
    }

    class PlaybackListener implements PlaybackListenerInterface {
        @Override
        public void onDurationChanged(int duration) {
            playSeekBar.setMax(duration);
        }

        @Override
        public void onPositionChanged(int position) {
            if(!userIsSeeking) {
                if(Build.VERSION.SDK_INT >= 24)
                    playSeekBar.setProgress(position, true);
                else
                    playSeekBar.setProgress(position);
            }
        }

        @Override
        public void onPlaybackCompleted() {
            //a trebuit să folosesc asta pentru că în lolipop crăpa zicînd că nu am voie să updatez UI din thread secundar.
            playPause.post(() ->
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play))
            );
            player.reset();
        }

        @Override
        public void onError() {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
            playPause.setEnabled(false);
            resetPlaying.setEnabled(false);
            totalTime.setText("00:00");
            playSeekBar.setEnabled(false);
            recordingInfo.setText(getResources().getString(R.string.player_error));
            recordingInfo.setTextColor(getResources().getColor(R.color.red));
            volumeControl.setEnabled(false);
            gainControl.setEnabled(false);
        }

        @Override
        public void onReset() {
            player = new AudioPlayer(new PlaybackListener());
            if(player.loadMedia(recording.getPath()))
                player.setGain(gainControl.getProgress());
        }
    }
}
