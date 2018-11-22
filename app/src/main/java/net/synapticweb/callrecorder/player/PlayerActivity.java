package net.synapticweb.callrecorder.player;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import net.synapticweb.callrecorder.AppLibrary;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;
import net.synapticweb.callrecorder.contactdetail.ContactDetailPresenter;
import net.synapticweb.callrecorder.data.Recording;


public class PlayerActivity extends TemplateActivity {
    ImageButton playPause, stopPlaying, closeBtn;
    SeekBar playSeekBar;
    MediaPlayerHolder mediaPlayerHolder = null;
    TextView playedTime, totalTime;
    boolean userIsSeeking = false;
    private Recording recording;
    private static final int WINDOW_HEIGHT_DP = 250;
    private static final String TAG = "CallRecorder";

    @Override
    protected Fragment createFragment() {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.wtf(TAG, "onCreate");
        setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_activity);

        //https://stackoverflow.com/questions/1979369/android-activity-as-a-dialog
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, (int) (WINDOW_HEIGHT_DP * metrics.density));

        Toolbar toolbar = findViewById(R.id.toolbar_play);
        recording = getIntent().getParcelableExtra(ContactDetailPresenter.RECORDING_EXTRA);
        toolbar.setTitle("Playing " + recording.getDate() + " " + recording.getTime());
        setSupportActionBar(toolbar);

        playPause = findViewById(R.id.player_button_play_pause);
        stopPlaying = findViewById(R.id.player_button_stop);
        playSeekBar = findViewById(R.id.play_seekbar);
        playedTime = findViewById(R.id.play_time_played);
        totalTime = findViewById(R.id.play_total_time);
        closeBtn = findViewById(R.id.close_player);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayerHolder.isPlaying()) {
                    mediaPlayerHolder.pause();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                }
                else {
                    mediaPlayerHolder.play();
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
                }
            }
        });

        stopPlaying.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mediaPlayerHolder.isPlaying())
                    playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
                mediaPlayerHolder.reset();
            }
        });

        playSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userSelectedPosition = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    userSelectedPosition = progress;
                playedTime.setText(AppLibrary.getDurationHuman(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userIsSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userIsSeeking = false;
                mediaPlayerHolder.seekTo(userSelectedPosition);
            }
        });

        mediaPlayerHolder = new MediaPlayerHolder(this);
        mediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
    }


    @Override
    public void onStart() {
        Log.wtf(TAG, "onStart");
        super.onStart();
        mediaPlayerHolder.loadMedia(recording.getPath());

        playedTime.setText("00:00");
        totalTime.setText(AppLibrary.getDurationHuman(mediaPlayerHolder.getTotalDuration()));

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int currentPosition = pref.getInt("player_currentPosition", 0);
        boolean isPlaying = pref.getBoolean("player_isPlaying", true);
        mediaPlayerHolder.setMediaPosition(currentPosition);
        if(isPlaying) {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_pause));
            mediaPlayerHolder.play();
        }
        else
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
    }

    @Override
    public void onStop() {
        Log.wtf(TAG, "onStop");
        super.onStop();
        if(!(isChangingConfigurations() && mediaPlayerHolder.isPlaying())) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("player_currentPosition", mediaPlayerHolder.getCurrentPosition());
            editor.putBoolean("player_isPlaying", mediaPlayerHolder.isPlaying());
            editor.apply();
            mediaPlayerHolder.release();
        }
    }

    @Override
    protected void onDestroy() {
        Log.wtf(TAG, "onDestroy");
        super.onDestroy();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("player_isPlaying");
        editor.remove("player_currentPosition");
        editor.apply();
    }

    public class PlaybackListener extends PlaybackInfoListener {
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
            //dacă pun aici codul pentru updatarea textului playedTime, nu se updatează nici textul și se oprește
            // și updatarea barei. Cauze necunoscute deocamdată. Mutat codul în playSeekBar.onProgressChanged().
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            Log.wtf(TAG, String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
            playPause.setBackground(getResources().getDrawable(R.drawable.player_play));
            mediaPlayerHolder.reset();
        }
    }
}
