package com.sceyt.sceytchatuikit.media.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioRecorderHelper {

    private final static Executor recorderExecutor = Executors.newSingleThreadScheduledExecutor();
    private static File audioFile;
    private static AudioRecorder currentRecorder;
    private static final Gson serializer = new GsonBuilder().create();

    public interface OnRecorderStart {
        void onStart(boolean started);
    }

    public interface OnRecorderStop {
        void onStop(boolean tooShort, File recordedFile, Integer duration, Integer[] amplitudes);
    }

    public interface OnRecorderCancel {
        void onCancel();
    }

    public static void startRecording(String directoryToSaveFile, OnRecorderStart onRecorderStart) {
        recorderExecutor.execute(() -> {
            audioFile = FileManager.createFile(AudioRecorderImpl.AUDIO_FORMAT, directoryToSaveFile);
            currentRecorder = new AudioRecorderImpl(audioFile);
            boolean started = currentRecorder.startRecording(32000, null);
            if (onRecorderStart != null) {
                onRecorderStart.onStart(started);
            }
        });
    }

    public static void stopRecording(OnRecorderStop onRecorderStop) {
        recorderExecutor.execute(() -> {
            currentRecorder.stopRecording();

            int duration = getCurrentDuration();
            boolean isTooShort = false;
            if (duration < 1) {
                audioFile.delete();
                isTooShort = true;
            }

            if (onRecorderStop != null) {
                onRecorderStop.onStop(isTooShort, audioFile, duration, getCurrentAmplitudes());
            }
        });
    }

    public static void cancelRecording(OnRecorderCancel onRecorderCancel) {
        recorderExecutor.execute(() -> {
            currentRecorder.stopRecording();
            audioFile.delete();
            if (onRecorderCancel != null)
                onRecorderCancel.onCancel();
        });
    }

    public static Integer[] getCurrentAmplitudes() {
        if (currentRecorder != null)
            return currentRecorder.getRecordingAmplitudes();
        else
            return new Integer[]{0};
    }

    public static Integer getCurrentDuration() {
        if (currentRecorder != null)
            return currentRecorder.getRecordingDuration();
        else
            return 0;
    }

    public static String getJsonAmplitudes() {
        Integer[] amps = getCurrentAmplitudes();
        return serializer.toJson(amps, Integer[].class);
    }

}
