package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.AttachmentDataItem
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

sealed class ChannelFileItem : AttachmentDataItem {

    private constructor()

    constructor(file: AttachmentWithUserData) : super(file.attachment)

    data class File(val data: AttachmentWithUserData) : ChannelFileItem(data)

    data class Image(val data: AttachmentWithUserData) : ChannelFileItem(data)

    data class Video(val data: AttachmentWithUserData) : ChannelFileItem(data)

    data class Voice(val data: AttachmentWithUserData) : ChannelFileItem(data)

    data class Link(val data: AttachmentWithUserData) : ChannelFileItem(data) {
        var linkPreviewMetaData: LinkPreviewHelper.PreviewMetaData? = null
    }

    object LoadingMoreItem : ChannelFileItem()

    companion object {
        fun ChannelFileItem.getData(): AttachmentWithUserData? {
            return when (this) {
                is File -> data
                is Image -> data
                is Link -> data
                is Video -> data
                is Voice -> data
                is LoadingMoreItem -> null
            }
        }
    }
}