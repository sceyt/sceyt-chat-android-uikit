package com.sceyt.sceytchatuikit.media.audio;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;

import com.sceyt.sceytchatuikit.presentation.common.ConcurrentHashSet;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import kotlin.Pair;

public class AudioPlayerImpl implements AudioPlayer {

    private final static int TIMER_PERIOD = 33;
    private final MediaPlayer player;
    private final String filePath;
    private long startTime;
    private Timer timer;
    private final ConcurrentHashMap<String, ConcurrentHashSet<Pair<String, AudioPlayerHelper.OnAudioPlayer>>> events = new ConcurrentHashMap<>();
    private boolean stopped;
    private float playbackSpeed = 1f;

    public AudioPlayerImpl(String audioFilePath, AudioPlayerHelper.OnAudioPlayer events, String tag) {
        this.filePath = audioFilePath;
        this.player = new MediaPlayer();

        addToEvents(audioFilePath, tag, events);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();

        this.player.setAudioAttributes(audioAttributes);
    }

    @Override
    public boolean initialize() {
        try {
            this.player.setDataSource(filePath);

            this.player.setOnPreparedListener(mp -> {
                for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
                    event.component2().onProgress(player.getCurrentPosition(), player.getDuration(), filePath);
            });

            this.player.setOnSeekCompleteListener(mp -> {
                for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
                    event.component2().onSeek(player.getCurrentPosition(), filePath);
            });

            this.player.setOnCompletionListener(mp -> {
                stopTimer();
                this.stopped = true;
                seekToPosition(0);
                for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
                    event.component2().onStop(filePath);
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
        startTime = System.currentTimeMillis();
        player.start();
        startTimer();
        for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
            event.component2().onToggle(player.isPlaying(), filePath);
    }

    @Override
    public void pause() {
        this.player.pause();
        stopTimer();
        for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
            event.component2().onPaused(filePath);
    }

    @Override
    public void stop() {
        stopTimer();
        this.player.stop();
        this.stopped = true;
        for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
            event.component2().onStop(filePath);
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
        if (player.isPlaying())
            pause();
        else if (startTime > 0 && !stopped) {
            player.start();
            startTimer();
        } else
            play();

        for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
            event.component2().onToggle(player.isPlaying(), filePath);
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        if (speed < 0.5f || speed > 2f)
            return;
        playbackSpeed = speed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.player.getAudioSessionId() > 0) {
                boolean isPlaying = this.player.isPlaying();
                this.player.setPlaybackParams(this.player.getPlaybackParams().setSpeed(speed));
                if (!isPlaying)
                    pause();

                for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
                    event.component2().onSpeedChanged(speed, filePath);
            }
        }
    }

    @Override
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    @Override
    public void addEventListener(AudioPlayerHelper.OnAudioPlayer event, String tag, String filePath) {
        addToEvents(filePath, tag, event);
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
                for (Pair<String, AudioPlayerHelper.OnAudioPlayer> event : getEvents(filePath))
                    event.component2().onProgress(AudioPlayerImpl.this.player.getCurrentPosition(),
                            AudioPlayerImpl.this.player.getDuration(), filePath);
            }
        }, TIMER_PERIOD, TIMER_PERIOD);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void addToEvents(String filePath, String tag, AudioPlayerHelper.OnAudioPlayer event) {
        ConcurrentHashSet<Pair<String, AudioPlayerHelper.OnAudioPlayer>> events = this.events.get(filePath);
        if (events == null) {
            events = new ConcurrentHashSet<>();
        }
        events.add(new Pair<>(tag, event));
        this.events.put(filePath, events);
    }

    private ConcurrentHashSet<Pair<String, AudioPlayerHelper.OnAudioPlayer>> getEvents(String filePath) {
        ConcurrentHashSet<Pair<String, AudioPlayerHelper.OnAudioPlayer>> events = this.events.get(filePath);
        if (events != null) return events;
        return new ConcurrentHashSet<>();
    }
}
