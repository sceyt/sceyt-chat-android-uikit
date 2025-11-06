package com.sceyt.chatuikit.presentation.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.toSpannableString
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.mappers.toSceytAttachment
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper.buildWithAttributes
import com.sceyt.chatuikit.presentation.custom_views.DecoratedTextView
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import java.io.File

fun SceytMessage?.setChannelMessageDateAndStatusIcon(
    decoratedTextView: DecoratedTextView,
    itemStyle: ChannelItemStyle,
    dateText: CharSequence,
    edited: Boolean,
    shouldShowStatus: Boolean,
) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming || !shouldShowStatus) {
        decoratedTextView.appearanceBuilder()
            .setText(dateText)
            .setLeadingIcon(null)
            .setTrailingIcon(null)
            .enableLeadingText(edited)
            .build()
            .apply()
        return
    }
    val icons = itemStyle.messageDeliveryStatusIcons
    val icon = when (deliveryStatus) {
        DeliveryStatus.Pending -> icons.pendingIcon
        DeliveryStatus.Sent -> icons.sentIcon
        DeliveryStatus.Received -> icons.receivedIcon
        DeliveryStatus.Displayed -> icons.displayedIcon
    }
    icon?.let {
        decoratedTextView.appearanceBuilder()
            .setText(dateText)
            .setLeadingIcon(it)
            .enableLeadingText(edited)
            .build()
            .apply(checkIgnoreHighlight(deliveryStatus))

        decoratedTextView.isVisible = true
    }
}

fun SceytMessage?.setChatMessageDateAndStatusIcon(
    decoratedTextView: DecoratedTextView,
    itemStyle: MessageItemStyle,
    dateText: CharSequence,
    edited: Boolean,
) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming || MessageTypeEnum.fromValue(type) == null) {
        decoratedTextView.appearanceBuilder()
            .setText(dateText)
            .setTextStyle(itemStyle.messageDateTextStyle)
            .setTrailingIcon(null)
            .enableLeadingText(edited)
            .setLeadingText(itemStyle.editedStateText)
            .setLeadingTextStyle(itemStyle.messageStateTextStyle)
            .setOverlayColor(itemStyle.onOverlayColor)
            .build()
            .apply()
        return
    }
    val icons = itemStyle.messageDeliveryStatusIcons
    val icon = when (deliveryStatus) {
        DeliveryStatus.Pending -> icons.pendingIcon
        DeliveryStatus.Sent -> icons.sentIcon
        DeliveryStatus.Received -> icons.receivedIcon
        DeliveryStatus.Displayed -> icons.displayedIcon
    }
    icon?.let {
        decoratedTextView.appearanceBuilder()
            .setText(dateText)
            .setTextStyle(itemStyle.messageDateTextStyle)
            .setTrailingIcon(it)
            .enableLeadingText(edited)
            .setLeadingText(itemStyle.editedStateText)
            .setLeadingTextStyle(itemStyle.messageStateTextStyle)
            .setOverlayColor(itemStyle.onOverlayColor)
            .build()
            .apply(checkIgnoreHighlight(deliveryStatus))
        decoratedTextView.isVisible = true
    }
}

private fun checkIgnoreHighlight(deliveryStatus: DeliveryStatus?): Boolean {
    return deliveryStatus == DeliveryStatus.Displayed
}

fun SceytMessage.getFormattedBodyWithAttachments(
    context: Context,
    mentionTextStyle: TextStyle,
    messageTypeIconProvider: VisualProvider<SceytMessage, Drawable?>?,
    attachmentNameFormatter: Formatter<SceytAttachment>,
    mentionUserNameFormatter: Formatter<SceytUser>,
    mentionClickListener: ((String) -> Unit)?,
): CharSequence {
    val body = when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() || attachments.firstOrNull()?.type == AttachmentTypeEnum.Link.value -> {
            buildWithAttributes(
                context = context,
                mentionTextStyle = mentionTextStyle,
                mentionUserNameFormatter = mentionUserNameFormatter,
                mentionClickListener = mentionClickListener
            )
        }

        attachments.size == 1 -> {
            body.ifEmpty { attachmentNameFormatter.format(context, attachments[0]) }
        }

        else -> context.getString(R.string.sceyt_file)
    }
    val messageTypeIcon = messageTypeIconProvider?.provide(context, this)
    return if (messageTypeIcon != null) {
        SpannableStringBuilder().apply {
            append(messageTypeIcon.toSpannableString())
            append(body.trim())
        }
    } else body.trim()
}

fun DraftMessage.getFormattedBodyWithAttachments(
    context: Context,
    mentionTextStyle: TextStyle,
    attachmentNameFormatter: Formatter<SceytAttachment>,
    mentionUserNameFormatter: Formatter<SceytUser>,
    mentionClickListener: ((String) -> Unit)?,
): SpannableString {
    val body = when {
        voiceAttachment != null -> {
            body.orEmpty().ifEmpty { context.getString(R.string.sceyt_voice) }
        }

        attachments.isNullOrEmpty() || attachments.firstOrNull()?.type == AttachmentTypeEnum.Link -> {
            buildWithAttributes(
                context = context,
                mentionTextStyle = mentionTextStyle,
                mentionUserNameFormatter = mentionUserNameFormatter,
                mentionClickListener = mentionClickListener
            )
        }

        attachments.size == 1 -> {
            body.orEmpty().ifEmpty {
                attachmentNameFormatter.format(context, attachments[0].toSceytAttachment())
            }
        }

        else -> {
            body.orEmpty().ifEmpty {
                context.getString(R.string.sceyt_files)
            }
        }
    }
    return SpannableString(body)
}

fun SceytAttachment?.getShowName(context: Context): String {
    this ?: return ""
    return when (type) {
        AttachmentTypeEnum.Video.value -> context.getString(R.string.sceyt_video)
        AttachmentTypeEnum.Image.value -> context.getString(R.string.sceyt_photo)
        AttachmentTypeEnum.Voice.value -> context.getString(R.string.sceyt_voice)
        AttachmentTypeEnum.File.value -> context.getString(R.string.sceyt_file)
        else -> name
    }
}

fun SceytAttachment?.isAttachmentExistAndFullyLoaded(loadedFile: File): File? {
    if (this == null) return null

    val loadedFileSize by lazy { getFileSize(loadedFile.path) }
    if (loadedFile.exists() && loadedFileSize != 0L && loadedFileSize == fileSize)
        return loadedFile

    return null
}

fun SceytMessage.isPending() = deliveryStatus == DeliveryStatus.Pending

fun MessageState.isDeletedOrHardDeleted() =
    this == MessageState.Deleted || this == MessageState.DeletedHard

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
        disableMentionsCount = message.disableMentionsCount,
        poll = message.poll,
        messageReactions = message.messageReactions,
        files = message.files,
    )
}