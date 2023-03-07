package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.adapter

import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataItem

sealed class MediaItem(val data: AttachmentWithUserData) : AttachmentDataItem(data.attachment) {

    data class Image(private val attachmentWithUserData: AttachmentWithUserData) : MediaItem(attachmentWithUserData)

    data class Video(private val attachmentWithUserData: AttachmentWithUserData) : MediaItem(attachmentWithUserData)
}