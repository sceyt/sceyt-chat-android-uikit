package com.sceyt.sceytchatuikit.media.audio

import com.sceyt.sceytchatuikit.media.DurationCallback

interface AudioRecorder {
    // Constructor accepts created File or file path
    fun startRecording(bitrate: Int, durationCallback: DurationCallback?): Boolean
    fun stopRecording()
    fun getRecordingDuration(): Int
    fun getRecordingAmplitudes(): Array<Int>
}