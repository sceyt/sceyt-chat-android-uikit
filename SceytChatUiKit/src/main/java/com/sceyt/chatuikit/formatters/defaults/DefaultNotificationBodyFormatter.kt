package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.graphics.Typeface
import androidx.core.text.buildSpannedString
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.presentation.extensions.getFormattedBodyWithAttachments
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.styles.common.TextStyle

open class DefaultNotificationBodyFormatter : Formatter<PushData> {

    override fun format(context: Context, from: PushData): CharSequence {
        val attachmentIcon = from.message.attachments?.firstOrNull()?.getEmojiIcon()

        val messageBody = from.message.getFormattedBodyWithAttachments(
            context = context,
            mentionTextStyle = TextStyle(style = Typeface.BOLD),
            mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
            attachmentNameFormatter = SceytChatUIKit.formatters.attachmentNameFormatter,
            mentionClickListener = null
        )

        val formattedBody = buildSpannedString {
            if (!attachmentIcon.isNullOrBlank()) {
                append(attachmentIcon)
                append(" ")
            }

            append(messageBody)
        }
        return when (from.type) {
            NotificationType.ChannelMessage -> formattedBody
            NotificationType.MessageReaction -> {
                buildSpannedString {
                    append("${context.getString(R.string.sceyt_reacted)} " +
                            "${from.reaction?.key} " +
                            "${context.getString(R.string.sceyt_to)} ")
                    append("\"")
                    append(formattedBody)
                    append("\"")
                }
            }
        }
    }

    private fun SceytAttachment.getEmojiIcon(): String? {
        return when (type) {
            AttachmentTypeEnum.Video.value -> "▶️"
            AttachmentTypeEnum.Image.value -> "🌄"
            AttachmentTypeEnum.Voice.value -> "🎤"
            AttachmentTypeEnum.File.value -> "📄"
            else -> null
        }
    }
}