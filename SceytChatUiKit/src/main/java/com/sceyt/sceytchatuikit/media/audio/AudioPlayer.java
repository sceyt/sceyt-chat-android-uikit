package com.sceyt.sceytchatuikit.media.audio;

public interface AudioPlayer {
    // Constructor accepts created File or file path

    boolean initialize();

    void play();

    void pause();

    void stop();

    long getPlaybackPosition();

    long getAudioDuration();

    void seekToPosition(long position);

    void togglePlayPause();

    void setPlaybackSpeed(float speed);

    String getFilePath();

    boolean isPlaying();

    void addEventListener(AudioPlayerHelper.OnAudioPlayer event, String tag);
}
