package com.sceyt.chatuikit.presentation.components.global_search

import android.graphics.Bitmap
import android.util.Size
import androidx.annotation.StringRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.persistence.mappers.toTransferData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentMetadataPayload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentUpdater
import com.sceyt.chatuikit.presentation.components.channel.messages.events.AttachmentDataProvider
import com.sceyt.chatuikit.presentation.custom_views.voice_recorder.AudioMetadata

enum class GlobalSearchTab(@param:StringRes val titleRes: Int) {
    Chats(R.string.sceyt_chats),
    Channels(R.string.sceyt_channels),
    Media(R.string.sceyt_media),
    Files(R.string.sceyt_files),
    Voice(R.string.sceyt_voice),
    Links(R.string.sceyt_links)
}

sealed interface GlobalSearchListItem {
    data class SectionHeader(@param:StringRes val titleRes: Int) : GlobalSearchListItem

    data class ChannelItem(
        val channel: SceytChannel
    ) : GlobalSearchListItem

    data class MessageItem(
        val result: GlobalSearchMessageResult,
        val query: String,
    ) : GlobalSearchListItem

    data class DateSeparator(val timestamp: Long) : GlobalSearchListItem

    data class AttachmentItem(
        val result: GlobalSearchAttachmentResult,
        val query: String,
    ) : GlobalSearchListItem, AttachmentDataProvider {
        private var _attachment: SceytAttachment = result.attachment
        private var _thumbPath: String? = null
        private var _transferData: TransferData? = result.attachment.toTransferData()
        private val _metadataPayload: AttachmentMetadataPayload = result.attachment.getInfoFromMetadata()

        override val attachment: SceytAttachment get() = _attachment
        override val size: Size? get() = _metadataPayload.size
        override val blurredThumb: Bitmap? get() = _metadataPayload.blurredThumbBitmap
        override val thumbPath: String? get() = _thumbPath
        override val duration: Long? get() = _metadataPayload.duration
        override val audioMetadata: AudioMetadata? get() = null
        override val transferData: TransferData? get() = _transferData

        override fun updateAttachment(file: SceytAttachment): SceytAttachment {
            _attachment = AttachmentUpdater.updateAttachment(_attachment, file)
            return _attachment
        }

        override fun updateTransferData(transferData: TransferData?) {
            _transferData = transferData
        }

        override fun updateThumbPath(thumbPath: String?) {
            _thumbPath = thumbPath
        }
    }

    fun getCreatedAt(): Long {
        return when (this) {
            is SectionHeader -> 0L
            is ChannelItem -> channel.createdAt
            is MessageItem -> result.message.createdAt
            is DateSeparator -> timestamp
            is AttachmentItem -> result.attachment.createdAt
        }
    }
}

fun SceytUser.displayName(): String {
    val fullName = fullName
    return when {
        fullName.isNotBlank() -> fullName
        username.isNotBlank() -> username
        else -> id
    }
}
