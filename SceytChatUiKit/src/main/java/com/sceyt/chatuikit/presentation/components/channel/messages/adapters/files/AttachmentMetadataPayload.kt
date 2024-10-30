package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.graphics.Bitmap
import android.os.Parcelable
import android.util.Size
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import kotlinx.parcelize.Parcelize

@Parcelize
data class AttachmentMetadataPayload(
        var size: Size? = null,
        var duration: Long? = null,
        var blurredThumbBitmap: Bitmap? = null,
        var audioMetadata: AudioMetadata? = null
) : Parcelable
