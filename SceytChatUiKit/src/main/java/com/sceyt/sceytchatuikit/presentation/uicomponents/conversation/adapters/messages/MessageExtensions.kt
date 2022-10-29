package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages

import android.content.Context
import com.google.gson.Gson
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentMetadata
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.sceytchatuikit.persistence.mappers.toReactionEntity
import java.io.File

fun SceytMessage.getShowBody(context: Context): String {
    return when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() -> body.trim()
        else -> context.getString(R.string.sceyt_attachment)
    }
}

fun Message.isTextMessage() = attachments.isNullOrEmpty()

fun SceytMessage.getAttachmentUrl(context: Context): String? {
    if (!attachments.isNullOrEmpty()) {
        attachments!![0].apply {
            if (type.isEqualsVideoOrImage()) {
                val file = getLocaleFileByNameOrMetadata(File(context.filesDir, name))
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
    } catch (_: Exception) {
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
        avatarChanged = from?.avatarURL.equalsIgnoreNull(other.from?.avatarURL).not(),
        nameChanged = from?.fullName.equalsIgnoreNull(other.from?.fullName).not(),
        replayCountChanged = replyCount != other.replyCount,
        replayContainerChanged = parent != other.parent || parent?.from != other.parent?.from,
        reactionsChanged = lastReactions?.map { it.toReactionEntity(id) }.equalsIgnoreNull(
            other.lastReactions?.map { it.toReactionEntity(id) }
        ).not(),
        showAvatarAndNameChanged = canShowAvatarAndName != other.canShowAvatarAndName,
        filesChanged = attachments.equalsIgnoreNull(other.attachments).not()
    )
}

internal fun SceytMessage.diffContent(other: SceytMessage): MessageItemPayloadDiff {
    return MessageItemPayloadDiff(
        edited = state != other.state,
        bodyChanged = body != other.body,
        statusChanged = deliveryStatus != other.deliveryStatus,
        avatarChanged = from?.avatarURL.equalsIgnoreNull(other.from?.avatarURL).not(),
        nameChanged = from?.fullName.equalsIgnoreNull(other.from?.fullName).not(),
        replayCountChanged = replyCount != other.replyCount,
        replayContainerChanged = parent != other.parent || parent?.from != other.parent?.from,
        reactionsChanged = lastReactions?.map { it.toReactionEntity(id) }.equalsIgnoreNull(
            other.lastReactions?.map { it.toReactionEntity(id) }
        ).not(),
        showAvatarAndNameChanged = false,
        filesChanged = attachments.equalsIgnoreNull(other.attachments).not()
    )
}