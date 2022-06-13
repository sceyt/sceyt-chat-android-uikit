package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import com.google.gson.Gson
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import java.io.File


fun FileListItem.getFileFromMetadata(): File? {
    val metadata = file?.metadata ?: return null
    try {
        val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
        return File(data.localPath)
    } catch (e: Exception) {
    }
    return null
}