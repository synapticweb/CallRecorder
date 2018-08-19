package net.synapticweb.callrecorder.player;

interface PlayerAdapter {
    void setMediaPosition(int position);

    void loadMedia(String mediaPath);

    void release();

    void play();

    void reset();

    void pause();

    boolean isPlaying();

    void seekTo(int position);

    int getCurrentPosition();

    int getTotalDuration();
}
