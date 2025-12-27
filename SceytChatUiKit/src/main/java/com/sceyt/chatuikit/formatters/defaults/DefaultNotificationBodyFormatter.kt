package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.graphics.Typeface
import androidx.core.text.buildSpannedString
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.whitSpace
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
            when (from.message.type) {
                SceytMessageType.Poll.value -> {
                    append(SceytConstants.emojiPoll.whitSpace())
                }

                SceytMessageType.ViewOnce.value -> {
                    append(SceytConstants.emojiViewOnce.whitSpace())
                }

                else -> {
                    if (!attachmentIcon.isNullOrBlank())
                        append(attachmentIcon.whitSpace())
                }
            }

            append(messageBody)
        }
        return when (from.type) {
            NotificationType.ChannelMessage -> formattedBody
            NotificationType.MessageReaction -> {
                buildSpannedString {
                    append(
                        context.getString(R.string.sceyt_reacted).whitSpace() +
                                from.reaction?.key?.whitSpace() +
                                context.getString(R.string.sceyt_to).whitSpace()
                    )
                    append("\"")
                    append(formattedBody)
                    append("\"")
                }
            }
        }
    }

    private fun SceytAttachment.getEmojiIcon(): String? {
        return when (type) {
            AttachmentTypeEnum.Video.value -> SceytConstants.emojiVideo
            AttachmentTypeEnum.Image.value -> SceytConstants.emojiImage
            AttachmentTypeEnum.Voice.value -> SceytConstants.emojiVoice
            AttachmentTypeEnum.File.value -> SceytConstants.emojiFile
            else -> null
        }
    }
}