package com.sceyt.chatuikit.media.audio

fun interface ReachedMaxDurationListener {
    fun onReached(duration: Int)
}

interface AudioRecorder {
    fun startRecording(reachedMaxDurationListener: ReachedMaxDurationListener?): Boolean
    fun stopRecording()
    fun getRecordingDuration(): Int
    fun getRecordingAmplitudes(): Array<Int>
}