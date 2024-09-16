package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import android.os.Parcelable
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import kotlinx.parcelize.Parcelize


sealed class FileListItem : com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentDataItem, Parcelable {

    private constructor() : super()

    constructor(file: SceytAttachment) : super(file)

    @Parcelize
    data class File(val attachment: SceytAttachment) : FileListItem(attachment)

    @Parcelize
    data class Image(val attachment: SceytAttachment) : FileListItem(attachment)

    @Parcelize
    data class Video(val attachment: SceytAttachment) : FileListItem(attachment)

    @Parcelize
    data class Voice(val attachment: SceytAttachment) : FileListItem(attachment)

    @Parcelize
    data object LoadingMoreItem : FileListItem()
}


