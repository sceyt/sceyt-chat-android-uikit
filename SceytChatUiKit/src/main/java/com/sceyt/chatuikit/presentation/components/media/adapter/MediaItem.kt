package com.sceyt.chatuikit.presentation.components.media.adapter

import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentMetadataPayload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentUpdater
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataUpdater
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata

data class MediaItem(
        var data: AttachmentWithUserData,
        val type: MediaItemType,
        private var _dataFromJson: AttachmentMetadataPayload,
        private var _thumbPath: String?,
        private var _transferData: TransferData?,
) : AttachmentDataProvider, AttachmentDataUpdater {

    override val attachment: SceytAttachment
        get() = data.attachment

    override val size: Size?
        get() = _dataFromJson.size

    override val blurredThumb: Bitmap?
        get() = _dataFromJson.blurredThumbBitmap

    override val thumbPath: String?
        get() = _thumbPath

    override val duration: Long?
        get() = _dataFromJson.duration

    override val audioMetadata: AudioMetadata?
        get() = _dataFromJson.audioMetadata

    override val transferData: TransferData?
        get() = _transferData

    override fun updateAttachment(file: SceytAttachment): SceytAttachment {
        val updated = AttachmentUpdater.updateAttachment(data.attachment, file)
        data = data.copy(attachment = updated)
        return data.attachment
    }

    override fun updateTransferData(transferData: TransferData?) {
        _transferData = transferData
    }

    override fun updateThumbPath(thumbPath: String?) {
        _thumbPath = thumbPath
    }
}

enum class MediaItemType {
    Image, Video
}