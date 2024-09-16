package com.sceyt.chatuikit.presentation.components.media.adapter

import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData

sealed class MediaItem(val data: AttachmentWithUserData) : com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentDataItem(data.attachment) {

    data class Image(private val attachmentWithUserData: AttachmentWithUserData) : MediaItem(attachmentWithUserData)

    data class Video(private val attachmentWithUserData: AttachmentWithUserData) : MediaItem(attachmentWithUserData)
}