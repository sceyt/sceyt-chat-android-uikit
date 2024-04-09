package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed class FileListItem : AttachmentDataItem {
    lateinit var sceytMessage: SceytMessage

    private constructor() : super()

    constructor(file: SceytAttachment, sceytMessage: SceytMessage) : super(file) {
        this.sceytMessage = sceytMessage
    }

    data class File(val attachment: SceytAttachment,
                    val message: SceytMessage) : FileListItem(attachment, message)

    data class Image(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Video(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Voice(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data object LoadingMoreItem : FileListItem()
}


