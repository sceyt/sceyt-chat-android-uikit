package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import android.content.Context
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.extensions.getFileSize
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import java.io.File

fun Message.getShowBody(context: Context): String {
    return when {
        attachments.isNullOrEmpty() -> body.trim()
        else -> context.getString(R.string.attachment)
    }
}

fun Message.isTextMessage() = attachments.isNullOrEmpty()

fun Message.getAttachmentUrl(context: Context): String? {
    if (!attachments.isNullOrEmpty()) {
        attachments[0].apply {
            if (type.isEqualsVideoOrImage()) {
                val file = getLocaleFileByNameOrMetadata(File(context.filesDir, name))
                return if (file != null) file.path
                else url
            }
        }
    }
    return null
}

fun Attachment?.getFileFromMetadata(): File? {
    val metadata = this?.metadata ?: return null
    try {
        val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
        return File(data.localPath)
    } catch (e: Exception) {
    }
    return null
}

fun Attachment?.getLocaleFileByNameOrMetadata(loadedFile: File): File? {
    if (this == null) return null
    val fileFromMetadata = getFileFromMetadata()
    if (fileFromMetadata != null && fileFromMetadata.exists())
        return fileFromMetadata
    else {
        if (loadedFile.exists() && getFileSize(loadedFile.path) == uploadedFileSize)
            return loadedFile
    }
    return null
}