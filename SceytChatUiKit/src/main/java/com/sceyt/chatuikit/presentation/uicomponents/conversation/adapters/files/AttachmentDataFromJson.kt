package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files

import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.presentation.customviews.voicerecorder.AudioMetadata

data class AttachmentDataFromJson(
        var size: Size? = null,
        var duration: Long? = null,
        var blurredThumbBitmap: Bitmap? = null,
        var audioMetadata: AudioMetadata? = null
)
