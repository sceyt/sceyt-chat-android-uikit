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

    data class DateSeparator(
            var data: AttachmentWithUserData,
    ) : ChannelFileItem {

        override val attachment: SceytAttachment
            get() = data.attachment

        override val size get() = null
        override val blurredThumb get() = null
        override val thumbPath get() = null
        override val duration get() = null
        override val audioMetadata get() = null
        override val transferData get() = null

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            data = data.copy(attachment = file)
            return data.attachment
        }

        override fun updateTransferData(transferData: TransferData?) {
        }

        override fun updateThumbPath(thumbPath: String?) {
        }

        override fun equals(other: Any?): Boolean {
            return other is DateSeparator && other.data.attachment.createdAt == data.attachment.createdAt
        }

        override fun hashCode(): Int {
            return data.attachment.createdAt.hashCode()
        }
    }

    data object LoadingMoreItem : ChannelFileItem {
        override val attachment: SceytAttachment
            get() = throw IllegalStateException("LoadingMoreItem has no attachment")
        override val size get() = null
        override val blurredThumb get() = null
        override val thumbPath get() = null
        override val duration get() = null
        override val audioMetadata get() = null
        override val transferData get() = null

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            throw IllegalStateException("LoadingMoreItem has no attachment")
        }

        override fun updateThumbPath(thumbPath: String?) {
        }

        override fun updateTransferData(transferData: TransferData?) {
        }
    }

    fun getCreatedAt(): Long {
        return when (this) {
            is Item -> data.attachment.createdAt
            is DateSeparator -> attachment.createdAt
            else -> 0
        }
    }

    fun getItemData(): AttachmentWithUserData? {
        return when (this) {
            is Item -> data
            is DateSeparator -> data
            else -> null
        }
    }

    fun isMediaItem() = this !is LoadingMoreItem && this !is DateSeparator
}


enum class ChannelFileItemType {
    File, Image, Video, Voice, Link
}