/*
 * Copyright (C) 2019 Eugen Rădulescu <synapticwebb@gmail.com> - All rights reserved.
 *
 * You may use, distribute and modify this code only under the conditions
 * stated in the SW Call Recorder license. You should have received a copy of the
 * SW Call Recorder license along with this file. If not, please write to <synapticwebb@gmail.com>.
 */

package net.synapticweb.callrecorder.player;

public interface PlaybackListenerInterface {

    void onDurationChanged(int duration);

    void onPositionChanged(int position);

    void onPlaybackCompleted();

    void onError();

    void onReset();
}
