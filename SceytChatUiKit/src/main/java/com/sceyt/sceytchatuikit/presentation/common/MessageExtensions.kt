package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentMetadata
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.isEqualsVideoOrImage
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.sceytchatuikit.persistence.mappers.toReactionEntity
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import java.io.File

fun SceytMessage?.setChannelMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView, dateText: String, edited: Boolean) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming) {
        dateStatusView.setDateAndStatusIcon(dateText, null, edited)
        return
    }
    val iconResId = when (deliveryStatus) {
        DeliveryStatus.Pending -> ChannelStyle.statusIndicatorPendingIcon
        DeliveryStatus.Sent -> ChannelStyle.statusIndicatorSentIcon
        DeliveryStatus.Delivered -> ChannelStyle.statusIndicatorDeliveredIcon
        DeliveryStatus.Read -> ChannelStyle.statusIndicatorReadIcon
        DeliveryStatus.Failed -> R.drawable.sceyt_ic_status_faild
        else -> null
    }
    iconResId?.let {
        dateStatusView.setDateAndStatusIcon(dateText, dateStatusView.context.getCompatDrawable(it), edited)
        dateStatusView.isVisible = true
    }
}


fun SceytMessage?.setConversationMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView, dateText: String, edited: Boolean) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming) {
        dateStatusView.setDateAndStatusIcon(dateText, null, edited)
        return
    }
    val iconResId = when (deliveryStatus) {
        DeliveryStatus.Pending -> MessagesStyle.messageStatusPendingIcon
        DeliveryStatus.Sent -> MessagesStyle.messageStatusSentIcon
        DeliveryStatus.Delivered -> MessagesStyle.messageStatusDeliveredIcon
        DeliveryStatus.Read -> MessagesStyle.messageStatusReadIcon
        DeliveryStatus.Failed -> R.drawable.sceyt_ic_status_faild
        else -> null
    }
    iconResId?.let {
        dateStatusView.setDateAndStatusIcon(dateText, dateStatusView.context.getCompatDrawable(it), edited)
        dateStatusView.isVisible = true
    }
}

fun SceytMessage.getShowBody(context: Context): String {
    return when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() -> body.trim()
        attachments?.size == 1 -> attachments?.getOrNull(0).getShowName(context)
        else -> context.getString(R.string.sceyt_attachment)
    }
}

fun SceytAttachment?.getShowName(context: Context): String {
    this ?: return ""
    return when (type) {
        "video" -> context.getString(R.string.sceyt_video)
        "image" -> context.getString(R.string.sceyt_image)
        else -> name
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

private fun String?.getFileFromMetadata(): File? {
    val metadata = this ?: return null
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

    /*val fileFromMetadata = metadata.getFileFromMetadata()
    if (fileFromMetadata != null && fileFromMetadata.exists())
        return fileFromMetadata*/

    return null
}

internal fun SceytMessage.diff(other: SceytMessage): MessageItemPayloadDiff {
    return MessageItemPayloadDiff(
        edited = state != other.state,
        bodyChanged = body != other.body,
        statusChanged = deliveryStatus != other.deliveryStatus,
        avatarChanged = from?.avatarURL.equalsIgnoreNull(other.from?.avatarURL).not(),
        nameChanged = from?.fullName.equalsIgnoreNull(other.from?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = parent != other.parent || parent?.from != other.parent?.from,
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
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = parent != other.parent || parent?.from != other.parent?.from,
        reactionsChanged = lastReactions?.map { it.toReactionEntity(id) }.equalsIgnoreNull(
            other.lastReactions?.map { it.toReactionEntity(id) }
        ).not(),
        showAvatarAndNameChanged = false,
        filesChanged = attachments.equalsIgnoreNull(other.attachments).not()
    )
}