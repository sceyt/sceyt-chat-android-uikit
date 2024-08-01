package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import kotlinx.parcelize.Parcelize


sealed class FileListItem : AttachmentDataItem, Parcelable {
    lateinit var sceytMessage: SceytMessage

    private constructor() : super()

    constructor(file: SceytAttachment, sceytMessage: SceytMessage) : super(file) {
        this.sceytMessage = sceytMessage
    }

    @Parcelize
    data class File(val attachment: SceytAttachment,
                    val message: SceytMessage) : FileListItem(attachment, message)

    @Parcelize
    data class Image(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    @Parcelize
    data class Video(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    @Parcelize
    data class Voice(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    @Parcelize
    data object LoadingMoreItem : FileListItem()
}


