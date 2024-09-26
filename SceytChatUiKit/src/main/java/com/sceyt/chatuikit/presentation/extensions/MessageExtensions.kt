package com.sceyt.chatuikit.presentation.extensions

import android.content.Context
import android.text.SpannableString
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper.buildWithAttributes
import com.sceyt.chatuikit.presentation.custom_views.DecoratedTextView
import com.sceyt.chatuikit.styles.ChannelItemStyle
import com.sceyt.chatuikit.styles.MessageItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import java.io.File

fun SceytMessage?.setChannelMessageDateAndStatusIcon(
        decoratedTextView: DecoratedTextView,
        itemStyle: ChannelItemStyle,
        dateText: CharSequence,
        edited: Boolean,
        shouldShowStatus: Boolean
) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming || !shouldShowStatus) {
        decoratedTextView.setTextAndIcons(
            text = dateText,
            leadingIcon = null,
            trailingIcon = null,
            enableLeadingText = edited, ignoreHighlight = false)
        return
    }
    val icons = itemStyle.messageDeliveryStatusIcons
    val icon = when (deliveryStatus) {
        DeliveryStatus.Pending -> icons.pendingIcon
        DeliveryStatus.Sent -> icons.sentIcon
        DeliveryStatus.Received -> icons.receivedIcon
        DeliveryStatus.Displayed -> icons.displayedIcon
        else -> null
    }
    icon?.let {
        decoratedTextView.setTextAndIcons(
            text = dateText,
            leadingIcon = it,
            enableLeadingText = edited,
            ignoreHighlight = checkIgnoreHighlight(deliveryStatus))
        decoratedTextView.isVisible = true
    }
}

fun SceytMessage?.setConversationMessageDateAndStatusIcon(
        decoratedTextView: DecoratedTextView,
        itemStyle: MessageItemStyle,
        dateText: String,
        edited: Boolean
) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming) {
        decoratedTextView.setTextAndIcons(text = dateText,
            leadingIcon = null,
            trailingIcon = null,
            enableLeadingText = edited,
            leadingText = itemStyle.editedMessageStateText,
            ignoreHighlight = false)
        return
    }
    val icons = itemStyle.messageDeliveryStatusIcons
    val icon = when (deliveryStatus) {
        DeliveryStatus.Pending -> icons.pendingIcon
        DeliveryStatus.Sent -> icons.sentIcon
        DeliveryStatus.Received -> icons.receivedIcon
        DeliveryStatus.Displayed -> icons.displayedIcon
        else -> {
            SceytLog.e(TAG, "Unknown delivery status: $deliveryStatus for message: $id, tid: $tid, body: $body")
            null
        }
    }
    icon?.let {
        decoratedTextView.setTextAndIcons(text = dateText,
            textColor = itemStyle.messageDateTextColor,
            trailingIcon = it,
            enableLeadingText = edited,
            leadingText = itemStyle.editedMessageStateText,
            leadingTextStyle = itemStyle.messageEditedTextStyle,
            ignoreHighlight = checkIgnoreHighlight(deliveryStatus))
        decoratedTextView.isVisible = true
    }
}

private fun checkIgnoreHighlight(deliveryStatus: DeliveryStatus?): Boolean {
    return deliveryStatus == DeliveryStatus.Displayed
}

fun SceytMessage.getFormattedBody(
        context: Context,
        mentionTextStyle: TextStyle,
        attachmentNameFormatter: Formatter<SceytAttachment>
): SpannableString {
    val body = when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() || attachments.getOrNull(0)?.type == AttachmentTypeEnum.Link.value() -> {
            buildWithAttributes(context, mentionTextStyle)
        }

        attachments.size == 1 -> {
            body.ifEmpty { attachmentNameFormatter.format(context, attachments[0]) }
        }

        else -> context.getString(R.string.sceyt_file)
    }
    return SpannableString(body)
}

fun SceytAttachment?.getShowName(context: Context): String {
    this ?: return ""
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

fun SceytMessage.isPending() = deliveryStatus == DeliveryStatus.Pending

fun MessageState.isDeletedOrHardDeleted() = this == MessageState.Deleted || this == MessageState.DeletedHard

fun MessageState.isDeleted() = this == MessageState.Deleted

fun MessageState.isHardDeleted() = this == MessageState.DeletedHard

fun SceytMessage.getUpdateMessage(message: SceytMessage): SceytMessage {
    return copy(
        id = message.id,
        tid = message.tid,
        channelId = message.channelId,
        body = message.body,
        type = message.type,
        metadata = message.metadata,
        //createdAt = message.createdAt
        updatedAt = message.updatedAt,
        incoming = message.incoming,
        isTransient = message.isTransient,
        silent = message.silent,
        deliveryStatus = message.deliveryStatus,
        state = message.state,
        user = message.user,
        attachments = message.attachments,
        userReactions = message.userReactions,
        reactionTotals = message.reactionTotals,
        markerTotals = message.markerTotals,
        userMarkers = message.userMarkers,
        mentionedUsers = message.mentionedUsers,
        parentMessage = message.parentMessage,
        replyCount = message.replyCount,
        autoDeleteAt = message.autoDeleteAt,
        pendingReactions = message.pendingReactions,
        bodyAttributes = message.bodyAttributes,
        messageReactions = message.messageReactions,
        files = message.files
    )
}