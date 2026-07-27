package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.graphics.Bitmap
import android.os.Parcelable
import android.util.Size
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata
import kotlinx.parcelize.Parcelize

@Parcelize
class FileListItem(
    private var _attachment: SceytAttachment,
    private var _metadataPayload: AttachmentMetadataPayload,
    private var _thumbPath: String?,
    private var _transferData: TransferData?,
    val type: AttachmentTypeEnum,
) : AttachmentDataProvider, Parcelable {

    override val attachment: SceytAttachment
        get() = _attachment

    override val size: Size?
        get() = _metadataPayload.size

    override val blurredThumb: Bitmap?
        get() = _metadataPayload.blurredThumbBitmap

    override val thumbPath: String?
        get() = _thumbPath

    override val duration: Long?
        get() = _metadataPayload.duration

    override val audioMetadata: AudioMetadata?
        get() = _metadataPayload.audioMetadata

    override val transferData: TransferData?
        get() = _transferData

    override fun updateAttachment(file: SceytAttachment): SceytAttachment {
        _attachment = AttachmentUpdater.updateAttachment(_attachment, file)
        return _attachment
    }

    override fun updateThumbPath(thumbPath: String?) {
        _thumbPath = thumbPath
    }

    override fun updateTransferData(transferData: TransferData?) {
        _transferData = transferData
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileListItem) return false

        return _attachment == other._attachment
                && _metadataPayload == other._metadataPayload
                && _thumbPath == other._thumbPath
                && _transferData == other._transferData
                && type == other.type
    }

    override fun hashCode(): Int {
        var result = _attachment.hashCode()
        result = 31 * result + _metadataPayload.hashCode()
        result = 31 * result + (_thumbPath?.hashCode() ?: 0)
        result = 31 * result + (_transferData?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}
