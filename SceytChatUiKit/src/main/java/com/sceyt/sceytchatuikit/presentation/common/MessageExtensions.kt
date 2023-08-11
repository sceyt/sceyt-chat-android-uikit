package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import android.text.SpannableString
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.extensions.equalsIgnoreNull
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import java.io.File

fun SceytMessage?.setChannelMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView, dateText: String, edited: Boolean, shouldShowStatus: Boolean) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming || !shouldShowStatus) {
        dateStatusView.setDateAndStatusIcon(dateText, null, edited, false)
        return
    }
    val iconResId = when (deliveryStatus) {
        DeliveryStatus.Pending -> ChannelStyle.statusIndicatorPendingIcon
        DeliveryStatus.Sent -> ChannelStyle.statusIndicatorSentIcon
        DeliveryStatus.Received -> ChannelStyle.statusIndicatorDeliveredIcon
        DeliveryStatus.Displayed -> ChannelStyle.statusIndicatorReadIcon
        else -> null
    }
    iconResId?.let {
        dateStatusView.setDateAndStatusIcon(dateText, dateStatusView.context.getCompatDrawable(it), edited, checkIgnoreHighlight(deliveryStatus))
        dateStatusView.isVisible = true
    }
}


fun SceytMessage?.setConversationMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView, dateText: String,
                                                          edited: Boolean) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming) {
        dateStatusView.setDateAndStatusIcon(dateText, null, edited, false)
        return
    }
    val iconResId = when (deliveryStatus) {
        DeliveryStatus.Pending -> MessagesStyle.messageStatusPendingIcon
        DeliveryStatus.Sent -> MessagesStyle.messageStatusSentIcon
        DeliveryStatus.Received -> MessagesStyle.messageStatusDeliveredIcon
        DeliveryStatus.Displayed -> MessagesStyle.messageStatusReadIcon
        else -> null
    }
    iconResId?.let {
        dateStatusView.setDateAndStatusIcon(dateText, dateStatusView.context.getCompatDrawable(it), edited, checkIgnoreHighlight(deliveryStatus))
        dateStatusView.isVisible = true
    }
}

private fun checkIgnoreHighlight(deliveryStatus: DeliveryStatus?): Boolean {
    return deliveryStatus == DeliveryStatus.Displayed
}

fun SceytMessage.getShowBody(context: Context): SpannableString {
    val body = when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() || attachments?.getOrNull(0)?.type == AttachmentTypeEnum.Link.value() -> {
            MentionUserHelper.buildOnlyNamesWithMentionedUsers(body, metadata, mentionedUsers)
        }

        attachments?.size == 1 -> attachments?.getOrNull(0).getShowName(context, body)
        else -> context.getString(R.string.sceyt_file)
    }
    return SpannableString(body)
}

fun SceytAttachment?.getShowName(context: Context, body: String): String {
    this ?: return ""
    if (body.isNotNullOrBlank()) return body
    return when (type) {
        AttachmentTypeEnum.Video.value() -> context.getString(R.string.sceyt_video)
        AttachmentTypeEnum.Image.value() -> context.getString(R.string.sceyt_photo)
        AttachmentTypeEnum.Voice.value() -> context.getString(R.string.sceyt_voice)
        AttachmentTypeEnum.File.value() -> context.getString(R.string.sceyt_file)
        else -> name
    }
}

fun SceytMessage.isTextMessage() = attachments.isNullOrEmpty()

fun SceytAttachment?.checkLoadedFileIsCorrect(loadedFile: File): File? {
    if (this == null) return null

    if (loadedFile.exists() && getFileSize(loadedFile.path) == fileSize)
        return loadedFile

    return null
}

internal fun SceytMessage.diff(other: SceytMessage): MessageItemPayloadDiff {
    return MessageItemPayloadDiff(
        edited = state != other.state,
        bodyChanged = body != other.body,
        statusChanged = deliveryStatus != other.deliveryStatus,
        avatarChanged = user?.avatarURL.equalsIgnoreNull(other.user?.avatarURL).not(),
        nameChanged = user?.fullName.equalsIgnoreNull(other.user?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = parentMessage != other.parentMessage || parentMessage?.user != other.parentMessage?.user,
        reactionsChanged = messageReactions?.equalsIgnoreNull(other.messageReactions)?.not()
                ?: other.reactionTotals.isNullOrEmpty().not(),
        showAvatarAndNameChanged = canShowAvatarAndName != other.canShowAvatarAndName,
        filesChanged = attachments.equalsIgnoreNull(other.attachments).not()
    )
}

internal fun SceytMessage.diffContent(other: SceytMessage): MessageItemPayloadDiff {
    return MessageItemPayloadDiff(
        edited = state != other.state,
        bodyChanged = body != other.body,
        statusChanged = deliveryStatus != other.deliveryStatus,
        avatarChanged = user?.avatarURL.equalsIgnoreNull(other.user?.avatarURL).not(),
        nameChanged = user?.fullName.equalsIgnoreNull(other.user?.fullName).not(),
        replyCountChanged = replyCount != other.replyCount,
        replyContainerChanged = parentMessage != other.parentMessage || parentMessage?.user != other.parentMessage?.user,
        reactionsChanged = reactionTotals?.equalsIgnoreNull(other.reactionTotals)?.not()
                ?: other.reactionTotals.isNullOrEmpty().not(),
        showAvatarAndNameChanged = false,
        filesChanged = attachments.equalsIgnoreNull(other.attachments).not()
    )
}