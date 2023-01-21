package com.sceyt.sceytchatuikit.media.audio;


import com.sceyt.sceytchatuikit.media.DurationCallback;

public interface AudioRecorder {
    // Constructor accepts created File or file path

    boolean startRecording(Integer bitrate, DurationCallback durationCallback);

    void stopRecording();

    Integer getRecordingDuration();

    Integer[] getRecordingAmplitudes();
}
