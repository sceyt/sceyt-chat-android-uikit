package com.sceyt.sceytchatuikit.media.audio;

import static java.lang.Math.log10;

import android.media.MediaRecorder;
import android.util.Log;

import com.sceyt.sceytchatuikit.media.DurationCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class AudioRecorderImpl implements AudioRecorder {

    public final static String AUDIO_FORMAT = "m4a";
    private final static int MAX_TIME = 5 * 60 * 1000;
    private final static int TIMER_PERIOD = 33;

    private final File file;
    private MediaRecorder mediaRecorder;
    private volatile boolean recording;
    private Timer timer;
    private long startTime;
    private DurationCallback durationCallback;
    private final ArrayList<Integer> amplitudes;
    private int amplitudeIndex = 0;
    private final static int MAX_AMP_LEN = 50;


    public AudioRecorderImpl(File recordingFile) {
        //filePath = FileManager.createFile(AUDIO_FORMAT, FileManager.APP_AUDIO_DIRECTORY).getAbsolutePath();
        this.amplitudes = new ArrayList<>();
        this.file = recordingFile;
    }

    @Override
    public boolean startRecording(Integer bitrate, DurationCallback durationCallback) {
        try {
            Log.i("AudioRecorder", "startRecording");

//            this.recordingInterface = recordingInterface;
//            this.chatThreadData = chatThreadData;
            recording = true;

            this.durationCallback = durationCallback;

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(this.file.getAbsolutePath());
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.setMaxDuration(MAX_TIME);
            mediaRecorder.setAudioEncodingBitRate(bitrate);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                startTimer();  // move to after recording start
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }

            return true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void stopRecording() {
        Log.i("AudioRecorder", "stopRecording invoked: isRecording -> " + recording);
        if (recording) {
            stopTimer();

            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            mediaRecorder = null;
            recording = false;
        }
    }

    @Override
    public Integer getRecordingDuration() {
        int durationSec = (int) ((System.currentTimeMillis() - startTime) / 1000);
        return Math.max(durationSec, 0);
    }

    @Override
    public Integer[] getRecordingAmplitudes() {
        int totalSamples = amplitudes.size();
        float scaleFactor = Math.max(1, totalSamples / (float) MAX_AMP_LEN);

        Integer[] outputArray = new Integer[MAX_AMP_LEN];
        int outputIndex = 0;

        if (scaleFactor <= 1) {
            for (int i = 0; i < totalSamples; i++) {
                if (outputIndex == MAX_AMP_LEN)
                    break;
                outputArray[outputIndex++] = amplitudes.get(i);
            }
        } else {
            for (int i = 0; i < totalSamples; i++) {
                if (outputIndex == MAX_AMP_LEN)
                    break;

                if (i >= outputIndex * scaleFactor) {
                    outputArray[outputIndex++] = amplitudes.get(i);
                }
            }
        }

        if (outputIndex == 0) {
            return new Integer[]{0};
        } else {
            return Arrays.copyOf(outputArray, outputIndex);
        }
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
//                if (recordingInterface == null) {
//                    return;
//                }
//                long countDown = (System.currentTimeMillis() - startTime) / 1000;
//                format(countDown);
//                recordingInterface.recording(duration, mediaRecorder != null ? mediaRecorder.getMaxAmplitude() : 0);
//                if (countDown * 1000 >= MAX_TIME) {
//                    recordingInterface.onAutoStop(filePath);
//                    stopAndClear(chatThreadData.buddyNumberJid);
//                }


                amplitudeIndex++;
                int amplitude = mediaRecorder.getMaxAmplitude();
                if (amplitudeIndex == 1 && amplitude == 0) {
                    return;
                }

                float normAmplitude = amplitude * 160 / 32768f;
                int db = AmplitudeTodB(normAmplitude);

//                AppLog.e("amp: " + amplitude + " norm_amp: " + normAmplitude + " db: " + db);

                amplitudes.add(db);

            }
        }, TIMER_PERIOD, TIMER_PERIOD);
    }

    int AmplitudeTodB(float amplitude) {
        return clampDecibels(20.0f * log10(Math.abs(amplitude)));
    }

    int clampDecibels(double value) {
        return (int) Math.max(0, Math.min(160, value));
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

}
