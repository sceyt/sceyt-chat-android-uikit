package com.sceyt.sceytchatuikit.media.audio;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AudioPlayerImpl implements AudioPlayer {

    private final static int TIMER_PERIOD = 33;
    private final MediaPlayer player;
    private final String filePath;
    private long startTime;
    private Timer timer;
    private final AudioPlayerHelper.OnAudioPlayer events;
    private boolean stopped;

    public AudioPlayerImpl(String audioFilePath, AudioPlayerHelper.OnAudioPlayer events) {
        this.filePath = audioFilePath;
        this.player = new MediaPlayer();
        this.events = events;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();

        this.player.setAudioAttributes(audioAttributes);
    }

    public AudioPlayerHelper.OnAudioPlayer getEvents() {
        return events;
    }

    @Override
    public boolean initialize() {
        try {
            this.player.setDataSource(filePath);

            this.player.setOnPreparedListener(mp -> {
                this.events.onProgress(player.getCurrentPosition(), player.getDuration());
            });

            this.player.setOnSeekCompleteListener(mp -> {
                if (events != null)
                    events.onSeek(player.getCurrentPosition());
            });

            this.player.setOnCompletionListener(mp -> {
                stopTimer();
                this.stopped = true;
                if (events != null)
                    events.onStop();
            });

            this.player.prepare();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void play() {
        this.startTime = System.currentTimeMillis();
        this.player.start();
        startTimer();
        if (events != null) {
            events.onToggle(this.player.isPlaying());
        }
    }

    @Override
    public void pause() {
        this.player.pause();
    }

    @Override
    public void stop() {
        stopTimer();
        this.player.stop();
        this.stopped = true;
        if (events != null)
            events.onStop();
    }

    @Override
    public long getPlaybackPosition() {
        return this.player.getCurrentPosition();
    }

    @Override
    public long getAudioDuration() {
        return this.player.getDuration();
    }

    @Override
    public void seekToPosition(long position) {
        boolean wasPlaying = this.player.isPlaying();
        if (wasPlaying) {
            stopTimer();
            this.player.pause();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.player.seekTo(position, MediaPlayer.SEEK_CLOSEST);
        } else {
            this.player.seekTo((int) position);
        }

        if (wasPlaying) {
            startTimer();
            this.player.start();
        }
    }

    @Override
    public void togglePlayPause() {
        if (this.player.isPlaying())
            this.player.pause();
        else if (startTime > 0 && !stopped)
            this.player.start();
        else
            this.play();

        if (events != null) {
            events.onToggle(this.player.isPlaying());
        }
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        if (speed < 0.5f || speed > 2f)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.player.getAudioSessionId() > 0) {
                boolean isPlaying = this.player.isPlaying();
                this.player.setPlaybackParams(this.player.getPlaybackParams().setSpeed(speed));
                if (!isPlaying)
                    this.player.pause();

                if (events != null)
                    events.onSpeedChanged(speed);
            }

        }
    }

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    private void startTimer() {

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        startTime = System.currentTimeMillis();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (AudioPlayerImpl.this.events != null) {
                    events.onProgress(AudioPlayerImpl.this.player.getCurrentPosition(),
                            AudioPlayerImpl.this.player.getDuration());
                }
            }
        }, TIMER_PERIOD, TIMER_PERIOD);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
