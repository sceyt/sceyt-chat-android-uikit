package com.sceyt.sceytchatuikit.media.audio;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioPlayerHelper {

    private final static Executor playerExecutor = Executors.newSingleThreadScheduledExecutor();
    private static AudioPlayer currentPlayer;

    private static final ConcurrentHashMap<String, OnToggleCallback> playerToggleListeners = new ConcurrentHashMap<>();


    public interface OnAudioPlayer {
        void onInitialized();

        void onProgress(long position, long duration);

        default void onSeek(long position) {
        }

        void onToggle(boolean playing);

        void onStop();

        void onSpeedChanged(float speed);

        default void onError() {
        }
    }

    public static void init(String filePath, OnAudioPlayer events) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null) {
                if (currentPlayer.getFilePath().equals(filePath)) {
                    events.onInitialized();
                    return;
                }

                currentPlayer.stop();
            }

            currentPlayer = new AudioPlayerImpl(filePath, events);
            currentPlayer.initialize();

            if (events != null) {
                events.onInitialized();
            }
        });
    }

    public static void seek(String filePath, long position) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null && currentPlayer.getFilePath().equals(filePath))
                currentPlayer.seekToPosition(position);
        });
    }

    public static void play() {
        playerExecutor.execute(() -> {
            if (currentPlayer != null)
                currentPlayer.play();
        });
    }


    public static void stop(String filePath) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null && currentPlayer.getFilePath().equals(filePath)) {
                currentPlayer.stop();
                currentPlayer = null;
            }
        });
    }

    public static void stopAll() {
        playerExecutor.execute(() -> {
            if (currentPlayer != null) {
                currentPlayer.stop();
                currentPlayer = null;
            }
        });
    }

    public static void toggle(String filePath) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null && currentPlayer.getFilePath().equals(filePath)) {
                currentPlayer.togglePlayPause();
                for (OnToggleCallback callback : playerToggleListeners.values()) {
                    callback.onToggle();
                }
            }
        });
    }

    public static void setPlaybackSpeed(String filePath, float speed) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null && currentPlayer.getFilePath().equals(filePath)) {
                currentPlayer.setPlaybackSpeed(speed);
            }
        });
    }

    public static void addToggleCallback(String key, OnToggleCallback callback) {
        playerToggleListeners.put(key, callback);
    }

    public interface OnToggleCallback {
        void onToggle();
    }
}
