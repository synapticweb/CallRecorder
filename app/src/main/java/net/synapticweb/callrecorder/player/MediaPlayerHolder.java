package net.synapticweb.callrecorder.player;

import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//https://medium.com/google-developers/building-a-simple-audio-app-in-android-part-2-3-a514f6224b83

class MediaPlayerHolder implements PlayerAdapter {
    private MediaPlayer mediaPlayer;
    private PlaybackInfoListener playbackInfoListener;
    private static final String TAG = "CallRecorder";
    private ScheduledExecutorService executor;
    private Runnable seekbarPositionUpdateTask;
    private static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 500;
    private String mediaPath;
    private AppCompatActivity activity;

    MediaPlayerHolder(AppCompatActivity activity) {
        this.activity = activity;
    }

    private void initializeMediaPlayer() {
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopUpdatingCallbackWithPosition(true);
                    if(playbackInfoListener != null) {
                        playbackInfoListener.onStateChanged(PlaybackInfoListener.State.COMPLETED);
                        playbackInfoListener.onPlaybackCompleted();
                    }

                }
            });
        }
    }

    public void setMediaPosition(int position) {
        if(mediaPlayer != null && playbackInfoListener != null) {
            mediaPlayer.seekTo(position);
            playbackInfoListener.onPositionChanged(position);
        }
    }

    public void loadMedia(String mediaPath) {
        this.mediaPath = mediaPath;
        initializeMediaPlayer();

        try {
            mediaPlayer.setDataSource(mediaPath);
            mediaPlayer.prepare();
        }
        catch (IOException exc) {
            Log.wtf(TAG, exc.getMessage());
        }

        final int duration = mediaPlayer.getDuration();
        if(playbackInfoListener != null) {
            playbackInfoListener.onDurationChanged(duration);
            playbackInfoListener.onPositionChanged(0);
        }
    }

    public void release() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void play() {
        if(mediaPlayer != null && !mediaPlayer.isPlaying())
            mediaPlayer.start();
        if(playbackInfoListener != null)
            playbackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);

        startUpdatingCallBackWithPosition();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void reset() {
        if(mediaPlayer != null) {
            mediaPlayer.reset();
            loadMedia(mediaPath);
            if(playbackInfoListener != null)
                playbackInfoListener.onStateChanged(PlaybackInfoListener.State.RESET);

            stopUpdatingCallbackWithPosition(true);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if(playbackInfoListener != null)
                playbackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);

            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void seekTo(int position) {
        if(mediaPlayer != null)
            mediaPlayer.seekTo(position);
    }

    public void setPlaybackInfoListener(PlaybackInfoListener playbackInfoListener) {
        this.playbackInfoListener = playbackInfoListener;
    }

    private void startUpdatingCallBackWithPosition() {
        if(executor == null)
            executor = Executors.newSingleThreadScheduledExecutor();
        if(seekbarPositionUpdateTask == null)
            seekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    if(mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        if(playbackInfoListener != null)
                            playbackInfoListener.onPositionChanged(currentPosition);
                    }
                }
            };
        executor.scheduleAtFixedRate(seekbarPositionUpdateTask, 0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition) {
        if(executor != null) {
            executor.shutdownNow();
            executor = null;
            seekbarPositionUpdateTask = null;
            if(resetUIPlaybackPosition && playbackInfoListener != null)
                playbackInfoListener.onPositionChanged(0);
        }

    }

    public int getCurrentPosition() {
        if(mediaPlayer != null)
            return mediaPlayer.getCurrentPosition();
        return 0;
    }

    public int getTotalDuration() {
        if(mediaPlayer != null)
            return mediaPlayer.getDuration();
        return 0;
    }
}
