package com.sceyt.chatuikit.presentation.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.getFileSize
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.sceytstyles.ChannelListViewStyle
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle
import java.io.File

fun SceytMessage?.setChannelMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView,
                                                     channelStyle: ChannelListViewStyle,
                                                     dateText: String, edited: Boolean, shouldShowStatus: Boolean) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming || !shouldShowStatus) {
        dateStatusView.setDateAndStatusIcon(text = dateText,
            drawable = null,
            edited = edited,
            ignoreHighlight = false)
        return
    }
    val icon = when (deliveryStatus) {
        DeliveryStatus.Pending -> channelStyle.statusIndicatorPendingIcon
        DeliveryStatus.Sent -> channelStyle.statusIndicatorSentIcon
        DeliveryStatus.Received -> channelStyle.statusIndicatorDeliveredIcon
        DeliveryStatus.Displayed -> channelStyle.statusIndicatorReadIcon
        else -> null
    }
    icon?.let {
        dateStatusView.setDateAndStatusIcon(text = dateText,
            textColor = channelStyle.dateTextColor,
            drawable = it,
            edited = edited,
            ignoreHighlight = checkIgnoreHighlight(deliveryStatus))
        dateStatusView.isVisible = true
    }
}


fun SceytMessage?.setConversationMessageDateAndStatusIcon(dateStatusView: SceytDateStatusView,
                                                          style: MessageItemStyle,
                                                          dateText: String,
                                                          edited: Boolean) {
    if (this?.deliveryStatus == null || state == MessageState.Deleted || incoming) {
        dateStatusView.setDateAndStatusIcon(text = dateText,
            drawable = null,
            edited = edited,
            ignoreHighlight = false)
        return
    }
    val icon = when (deliveryStatus) {
        DeliveryStatus.Pending -> style.messageStatusPendingIcon
        DeliveryStatus.Sent -> style.messageStatusSentIcon
        DeliveryStatus.Received -> style.messageStatusDeliveredIcon
        DeliveryStatus.Displayed -> style.messageStatusReadIcon
        else -> {
            SceytLog.e(TAG, "Unknown delivery status: $deliveryStatus for message: $id, tid: $tid, body: $body")
            null
        }
    }
    icon?.let {
        dateStatusView.setDateAndStatusIcon(text = dateText,
            textColor = style.messageDateTextColor,
            drawable = it,
            edited = edited,
            editedText = style.editedMessageStateText,
            editedTextStyle = style.messageEditedTextStyle,
            ignoreHighlight = checkIgnoreHighlight(deliveryStatus))
        dateStatusView.isVisible = true
    }
}

private fun checkIgnoreHighlight(deliveryStatus: DeliveryStatus?): Boolean {
    return deliveryStatus == DeliveryStatus.Displayed
}

fun SceytMessage.getFormattedBody(context: Context): SpannableString {
    val body = when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() || attachments?.getOrNull(0)?.type == AttachmentTypeEnum.Link.value() -> {
            MessageBodyStyleHelper.buildWithMentionsAndAttributes(context, this)
        }

        attachments?.size == 1 -> {
            attachments?.getOrNull(0)?.getShowName(context, body)
        }

        else -> context.getString(R.string.sceyt_file)
    }
    return SpannableString(body)
}

fun SceytMessage.getFormattedLastMessageBody(context: Context): SpannableString {
    val body = when {
        state == MessageState.Deleted -> context.getString(R.string.sceyt_message_was_deleted)
        attachments.isNullOrEmpty() || attachments?.getOrNull(0)?.type == AttachmentTypeEnum.Link.value() -> {
            MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(this)
        }

        attachments?.size == 1 -> {
            attachments?.getOrNull(0)?.getShowName(context, body)
        }

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

fun SceytMessage.getAttachmentIconAsString(channelStyle: ChannelListViewStyle): SpannableStringBuilder {
    val icRes = getAttachmentIconId(channelStyle) ?: return SpannableStringBuilder()
    val builder = SpannableStringBuilder(". ")
    icRes.setBounds(0, 0, icRes.intrinsicWidth, icRes.intrinsicHeight)
    builder.setSpan(ImageSpan(icRes), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return builder
}

fun SceytMessage.getAttachmentIconId(channelStyle: ChannelListViewStyle): Drawable? {
    return attachments?.getOrNull(0)?.let {
        when (it.type) {
            AttachmentTypeEnum.Video.value() -> channelStyle.bodyVideoAttachmentIcon
            AttachmentTypeEnum.Image.value() -> channelStyle.bodyImageAttachmentIcon
            AttachmentTypeEnum.Voice.value() -> channelStyle.bodyVoiceAttachmentIcon
            AttachmentTypeEnum.File.value() -> channelStyle.bodyFileAttachmentIcon
            else -> null
        }
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