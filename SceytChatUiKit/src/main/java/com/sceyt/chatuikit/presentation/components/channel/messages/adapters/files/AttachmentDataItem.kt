package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata

open class AttachmentDataItem {
    lateinit var file: SceytAttachment
    var size: Size? = null
    var blurredThumb: Bitmap? = null
    var thumbPath: String? = null
    var duration: Long? = null
    var audioMetadata: AudioMetadata? = null

    val isFileItemInitialized: Boolean
        get() = ::file.isInitialized

    protected constructor()

    constructor(attachment: SceytAttachment) {
        file = attachment
        val data = attachment.getInfoFromMetadata()
        size = data.size
        blurredThumb = data.blurredThumbBitmap
        duration = data.duration
        audioMetadata = data.audioMetadata
    }
}