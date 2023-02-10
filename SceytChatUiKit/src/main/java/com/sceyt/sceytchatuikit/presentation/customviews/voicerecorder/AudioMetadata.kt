package com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder

data class AudioMetadata(
        val tmb: IntArray?,
        val dur: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioMetadata

        if (tmb != null) {
            if (other.tmb == null) return false
            if (!tmb.contentEquals(other.tmb)) return false
        } else if (other.tmb != null) return false
        if (dur != other.dur) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tmb?.contentHashCode() ?: 0
        result = 31 * result + dur
        return result
    }
}