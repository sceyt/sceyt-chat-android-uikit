package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata

data class AttachmentDataFromJson(
        var size: Size? = null,
        var duration: Long? = null,
        var blurredThumbBitmap: Bitmap? = null,
        var audioMetadata: AudioMetadata? = null
)
