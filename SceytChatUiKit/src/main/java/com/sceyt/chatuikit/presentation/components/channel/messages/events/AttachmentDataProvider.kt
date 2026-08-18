package com.sceyt.chatuikit.presentation.components.channel.messages.events

import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.mappers.getVideoThumbUrl
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata

interface AttachmentDataProvider : AttachmentDataUpdater {
    val attachment: SceytAttachment
    val size: Size?
    val blurredThumb: Bitmap?
    val thumbPath: String?
    val duration: Long?
    val audioMetadata: AudioMetadata?
    val transferData: TransferData?

    /** Url of the poster frame uploaded with the video, if the sender uploaded one. */
    val videoThumbUrl: String?
        get() = attachment.takeIf { it.type == AttachmentTypeEnum.Video.value }?.getVideoThumbUrl()

    val filePath: String?
        get() = attachment.filePath

    val messageTid: Long
        get() = attachment.messageTid
}

interface AttachmentDataUpdater {
    fun updateAttachment(file: SceytAttachment): SceytAttachment
    fun updateTransferData(transferData: TransferData?)
    fun updateThumbPath(thumbPath: String?)
}