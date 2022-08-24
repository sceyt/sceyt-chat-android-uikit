package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages

import android.content.Context
import com.google.gson.Gson
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.AttachmentMetadata
import com.sceyt.chat.ui.data.models.messages.SceytAttachment
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.getFileSize
import com.sceyt.chat.ui.extensions.isEqualsVideoOrImage
import java.io.File

fun SceytMessage.getShowBody(context: Context): String {
    return when {
        state == MessageState.Deleted -> context.getString(R.string.message_was_deleted)
        attachments.isNullOrEmpty() -> body.trim()
        else -> context.getString(R.string.attachment)
    }
}

fun Message.isTextMessage() = attachments.isNullOrEmpty()

fun SceytMessage.getAttachmentUrl(context: Context): String? {
    if (!attachments.isNullOrEmpty()) {
        attachments!![0].apply {
            if (type.isEqualsVideoOrImage()) {
                val file = getLocaleFileByNameOrMetadata(File(context.filesDir, name ?: ""))
                return if (file != null) file.path
                else url
            }
        }
    }
    return null
}

fun SceytAttachment?.getFileFromMetadata(): File? {
    val metadata = this?.metadata ?: return null
    try {
        val data = Gson().fromJson(metadata, AttachmentMetadata::class.java)
        return File(data.localPath)
    } catch (e: Exception) {
    }
    return null
}

fun SceytAttachment?.getLocaleFileByNameOrMetadata(loadedFile: File): File? {
    if (this == null) return null

    if (loadedFile.exists() && getFileSize(loadedFile.path) == fileSize)
        return loadedFile

    val fileFromMetadata = getFileFromMetadata()
    if (fileFromMetadata != null && fileFromMetadata.exists())
        return fileFromMetadata

    return null
}

internal fun SceytMessage.diff(other: SceytMessage): MessageItemPayloadDiff {
    return MessageItemPayloadDiff(
        edited = state != other.state,
        bodyChanged = body != other.body,
        statusChanged = deliveryStatus != other.deliveryStatus,
        avatarChanged = from?.avatarURL != other.from?.avatarURL,
        nameChanged = from?.fullName != other.from?.fullName,
        replayCountChanged = replyCount != other.replyCount,
        replayContainerChanged = parent != other.parent,
        reactionsChanged = messageReactions?.equals(other.messageReactions)?.not() ?: true,
        showAvatarAndNameChanged = canShowAvatarAndName != other.canShowAvatarAndName,
        filesChanged = !attachments.contentEquals(other.attachments)
    )
}