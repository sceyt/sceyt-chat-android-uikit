package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import com.sceyt.chat.models.attachment.Attachment

sealed class FileListItem {
    data class File(val file: Attachment?) : FileListItem()
    data class Image(val file: Attachment?) : FileListItem()
    data class Video(val file: Attachment?) : FileListItem()
}