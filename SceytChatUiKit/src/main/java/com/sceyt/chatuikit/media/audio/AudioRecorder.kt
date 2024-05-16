package com.sceyt.chatuikit.media.audio

import com.sceyt.chatuikit.media.DurationCallback

interface AudioRecorder {
    // Constructor accepts created File or file path
    fun startRecording(bitrate: Int, durationCallback: DurationCallback?): Boolean
    fun stopRecording()
    fun getRecordingDuration(): Int
    fun getRecordingAmplitudes(): Array<Int>
}