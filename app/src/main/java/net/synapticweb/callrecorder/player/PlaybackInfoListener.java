package net.synapticweb.callrecorder.player;

public interface PlaybackInfoListener {
    void onDurationChanged(int duration);

    void onPositionChanged(int position);

    void onPlaybackCompleted();

    void onInitializationError();

    void onReset();
}
