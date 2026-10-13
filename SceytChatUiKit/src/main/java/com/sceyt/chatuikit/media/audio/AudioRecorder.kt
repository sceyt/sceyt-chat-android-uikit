package com.sceyt.chatuikit.media.audio

fun interface ReachedMaxDurationListener {
    fun onReached(duration: Int)
}

fun interface RecorderErrorListener {
    fun onError(what: Int, extra: Int)
}

interface AudioRecorder {
    fun startRecording(
            reachedMaxDurationListener: ReachedMaxDurationListener?,
            errorListener: RecorderErrorListener? = null,
    ): Boolean

    fun stopRecording()
    fun getRecordingDuration(): Int
    fun getRecordingAmplitudes(): Array<Int>
    fun isRecording(): Boolean
}
