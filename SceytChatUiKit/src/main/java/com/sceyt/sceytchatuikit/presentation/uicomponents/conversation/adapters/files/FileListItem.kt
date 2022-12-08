package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

sealed class FileListItem() {
    lateinit var file: SceytAttachment
    lateinit var sceytMessage: SceytMessage

    constructor(file: SceytAttachment, sceytMessage: SceytMessage) : this() {
        this.file = file
        this.sceytMessage = sceytMessage
    }

    data class File(val attachment: SceytAttachment,
                    val message: SceytMessage) : FileListItem(attachment, message)

    data class Image(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Video(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    object LoadingMoreItem : FileListItem()
}


