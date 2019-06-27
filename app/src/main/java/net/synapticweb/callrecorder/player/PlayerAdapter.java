package net.synapticweb.callrecorder.player;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

interface PlayerAdapter {

    @IntDef({State.UNINITIALIZED, State.INITIALIZED, State.PLAYING, State.PAUSED })
    @Retention(RetentionPolicy.SOURCE)
    @interface State {
        int UNINITIALIZED = 0;
        int INITIALIZED = 1;
        int PLAYING = 2;
        int PAUSED = 3;
    }

    void setMediaPosition(int position);

    void loadMedia(String mediaPath);

    void release();

    void play();

    void reset();

    void pause();

    boolean isPlaying();

    void seekTo(int position);

    int getCurrentPosition();

    long getTotalDuration();
}
