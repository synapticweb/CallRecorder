/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

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
        int STOPPED = 4;
    }

    void setGain(float gain);

    boolean setMediaPosition(int position);

    boolean loadMedia(String mediaPath);

    void stopPlayer();

    void play();

    void reset();

    void pause();

    int getPlayerState();

    void setPlayerState(int state);

    boolean seekTo(int position);

    int getCurrentPosition();

    int getTotalDuration();
}
