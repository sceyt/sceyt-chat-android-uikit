package com.sceyt.sceytchatuikit.extensions

import android.widget.ImageView
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.presentation.customviews.voicerecorder.AudioMetadata

fun progressToMediaPlayerPosition(progress: Float, mediaDuration: Long): Long {
    return ((progress * mediaDuration) / 100f).toLong()
}

fun mediaPlayerPositionToSeekBarProgress(currentPosition: Long, mediaDuration: Long): Float {
    return (currentPosition * 100f / mediaDuration)
}

fun SceytAttachment.getMetadataFromAttachment(): AudioMetadata {
    return try {
        val result = Gson().fromJson(metadata, AudioMetadata::class.java)
        if (result.tmb == null) {
            // if thumb is null, should set it to an empty array
            result.copy(tmb = intArrayOf(0))
        } else {
            result
        }
    } catch (ex: JsonParseException) {
        null
    } ?: AudioMetadata(intArrayOf(0), 0)
}

fun setPlayButtonIcon(playing: Boolean, imageView: ImageView) {
    val iconRes = if (playing) R.drawable.sceyt_ic_pause else R.drawable.sceyt_ic_play
    imageView.setImageResource(iconRes)
}