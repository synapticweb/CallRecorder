package net.synapticweb.callrecorder.player;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.contactdetail.ContactDetailPresenter;
import net.synapticweb.callrecorder.data.Recording;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PlayerActivity extends TemplateActivity {
    AudioPlayer player;
    Recording recording;
    ImageButton playPause, resetPlaying;
    SeekBar playSeekBar;
    TextView playedTime, totalTime;
    boolean userIsSeeking = false;

    public Fragment createFragment() {return null;}

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setContentView(R.layout.player_activity);

        recording = getIntent().getParcelableExtra(ContactDetailPresenter.RECORDING_EXTRA);
        player = new AudioPlayer(new PlaybackListener());

        playPause = findViewById(R.id.test_player_play_pause);
        resetPlaying = findViewById(R.id.test_player_reset);
        playSeekBar = findViewById(R.id.test_play_seekbar);
        playedTime = findViewById(R.id.test_play_time_played);
        totalTime = findViewById(R.id.test_play_total_time);

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.isPlaying()) {
                    player.pause();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else {
                    player.play();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });

        resetPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player.isPlaying())
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                player.reset();
            }
        });

        playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userSelectedPosition = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    userSelectedPosition = progress;
                playedTime.setText(CrApp.getDurationHuman(progress, false));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { userIsSeeking = true; }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userIsSeeking = false;
                player.seekTo(userSelectedPosition);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        player.loadMedia(recording.getPath());
        playedTime.setText("00:00");
        totalTime.setText(CrApp.getDurationHuman(player.getTotalDuration(), false));
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int currentPosition = pref.getInt("player_currentPosition", 0);
        boolean isPlaying = pref.getBoolean("player_isPlaying", true);
        player.setMediaPosition(currentPosition);

        if(isPlaying) {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
            player.play();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("player_currentPosition", player.getCurrentPosition());
        editor.putBoolean("player_isPlaying", player.isPlaying());
        editor.apply();
        player.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //e necesar?
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("player_isPlaying");
        editor.remove("player_currentPosition");
        editor.apply();
    }

    class PlaybackListener implements PlaybackInfoListener {
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
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
            player.reset();
        }

        @Override
        public void onInitializationError() {
            playPause.setEnabled(false);
            resetPlaying.setEnabled(false);
            findViewById(R.id.player_error_message).setVisibility(View.VISIBLE);
        }

        @Override
        public void onReset() {
            player = new AudioPlayer(new PlaybackListener());
            player.loadMedia(recording.getPath());
        }
    }
}
