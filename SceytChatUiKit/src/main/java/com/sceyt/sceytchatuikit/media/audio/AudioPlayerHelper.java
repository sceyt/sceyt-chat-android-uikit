package com.sceyt.sceytchatuikit.media.audio;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioPlayerHelper {

    private final static Executor playerExecutor = Executors.newSingleThreadScheduledExecutor();
    private static AudioPlayer currentPlayer;
    private static final ConcurrentHashMap<String, OnToggleCallback> playerToggleListeners = new ConcurrentHashMap<>();

    public interface OnAudioPlayer {
        void onInitialized(boolean alreadyInitialized, AudioPlayer currentPlayer);

        void onProgress(long position, long duration);

        default void onSeek(long position) {
        }

        void onToggle(boolean playing);

        void onStop();

        void onSpeedChanged(float speed);

        default void onError() {
        }
    }

    public static void init(String filePath, OnAudioPlayer events, String tag) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null) {
                if (currentPlayer.getFilePath().equals(filePath)) {
                    events.onInitialized(true, currentPlayer);
                    currentPlayer.addEventListener(events, tag);
                    return;
                }

                currentPlayer.stop();
            }

            currentPlayer = new AudioPlayerImpl(filePath, events, tag);
            currentPlayer.initialize();

            if (events != null) {
                events.onInitialized(false, currentPlayer);
            }
        });
    }

    public static void addEventListener(OnAudioPlayer events, String tag) {
        playerExecutor.execute(() -> {
            if (currentPlayer != null) {
                currentPlayer.addEventListener(events, tag);
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

    public static String getCurrentPlayingAudioPath() {
        if (currentPlayer != null) {
            return currentPlayer.getFilePath();
        }
        return null;
    }

    @Nullable
    public static AudioPlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public static boolean alreadyInitialized(@NotNull String path) {
        if (currentPlayer == null) return false;
        return currentPlayer.getFilePath().equals(path);
    }

    public static boolean isPlaying(@NotNull String path) {
        if (currentPlayer == null) return false;
        return currentPlayer.getFilePath().equals(path) && currentPlayer.isPlaying();
    }

    public interface OnToggleCallback {
        void onToggle();
    }
}
