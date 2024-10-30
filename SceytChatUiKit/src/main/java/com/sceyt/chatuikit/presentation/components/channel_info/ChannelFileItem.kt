package com.sceyt.chatuikit.presentation.components.channel_info

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

sealed interface ChannelFileItem : AttachmentDataProvider, AttachmentDataUpdater {

    data class Item(
            var data: AttachmentWithUserData,
            val type: ChannelFileItemType,
            private var _metadataPayload: AttachmentMetadataPayload,
            private var _thumbPath: String?,
            private var _transferData: TransferData?,
    ) : ChannelFileItem {

        override val attachment: SceytAttachment
            get() = data.attachment

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

        override var transferData: TransferData?
            get() = _transferData
            set(value) {
                _transferData = value
            }

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

    data object LoadingMoreItem : ChannelFileItem {
        override val attachment: SceytAttachment
            get() = throw IllegalStateException("LoadingMoreItem has no attachment")
        override val size: Size
            get() = throw IllegalStateException("LoadingMoreItem has no size")
        override val blurredThumb: Bitmap
            get() = throw IllegalStateException("LoadingMoreItem has no blurredThumb")
        override val thumbPath: String
            get() = throw IllegalStateException("LoadingMoreItem has no thumbPath")
        override val duration: Long
            get() = throw IllegalStateException("LoadingMoreItem has no duration")
        override val audioMetadata: AudioMetadata
            get() = throw IllegalStateException("LoadingMoreItem has no audioMetadata")

        override var transferData: TransferData?
            get() = throw IllegalStateException("LoadingMoreItem has no transferData")
            set(value) {
                throw IllegalStateException("LoadingMoreItem has no transferData, couldn't set $value")
            }

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            throw IllegalStateException("LoadingMoreItem has no attachment")
        }

        override fun updateThumbPath(thumbPath: String?) {
            throw IllegalStateException("LoadingMoreItem has no thumbPath")
        }

        override fun updateTransferData(transferData: TransferData?) {
            throw IllegalStateException("LoadingMoreItem has no transferData")
        }
    }

    fun getCreatedAt(): Long {
        return (this as? Item)?.attachment?.createdAt ?: 0
    }

    fun getItemData(): AttachmentWithUserData? {
        return (this as? Item)?.data
    }

    fun isMediaItem() = this != LoadingMoreItem && (this as Item).type != ChannelFileItemType.MediaDate
}


enum class ChannelFileItemType {
    File, Image, Video, Voice, Link, MediaDate
}