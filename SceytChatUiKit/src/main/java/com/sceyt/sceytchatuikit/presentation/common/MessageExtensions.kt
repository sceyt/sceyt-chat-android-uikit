package com.sceyt.sceytchatuikit.presentation.common

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.getFileSize
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.sceytchatuikit.sceytstyles.ChannelStyle
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle
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

fun SceytMessage.getAttachmentIconAsString(context: Context): SpannableStringBuilder {
    val icRes = getAttachmentIconId() ?: return SpannableStringBuilder()
    val builder = SpannableStringBuilder(". ")
    builder.setSpan(ImageSpan(context, icRes), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return builder
}

fun SceytMessage.getAttachmentIconId(): Int? {
    return attachments?.getOrNull(0)?.let {
        when (it.type) {
            AttachmentTypeEnum.Video.value() -> ChannelStyle.bodyVideoAttachmentIcon
            AttachmentTypeEnum.Image.value() -> ChannelStyle.bodyImageAttachmentIcon
            AttachmentTypeEnum.Voice.value() -> ChannelStyle.bodyVoiceAttachmentIcon
            AttachmentTypeEnum.File.value() -> ChannelStyle.bodyFileAttachmentIcon
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