package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.PinnedMessageBodyFormatterAttributes
import com.sceyt.chatuikit.persistence.mappers.getInfoFromMetadata
import com.sceyt.chatuikit.presentation.extensions.isSupportedType

open class DefaultPinnedMessageBodyFormatter : Formatter<PinnedMessageBodyFormatterAttributes> {

    override fun format(context: Context, from: PinnedMessageBodyFormatterAttributes): CharSequence {
        val message = from.message

        val preview = when {
            message.state == MessageState.Deleted -> from.deletedStateText

            !message.isSupportedType() ->
                from.unsupportedMessageShortBodyFormatter.format(context, message)

            message.type == SceytMessageType.Poll.value -> formatPoll(context, message)

            else -> formatBodyOrAttachment(context, from, message)
        }

        return preview.takeIf { it.isNotBlank() } ?: context.getString(R.string.sceyt_message)
    }

    protected open fun formatPoll(context: Context, message: SceytMessage): CharSequence {
        val question = message.poll?.name?.takeIf { it.isNotBlank() } ?: message.body.trim()
        return context.getString(R.string.sceyt_pinned_poll, question)
    }

    protected open fun formatBodyOrAttachment(
        context: Context,
        from: PinnedMessageBodyFormatterAttributes,
        message: SceytMessage,
    ): CharSequence {
        val body = message.body.trim()
        val attachment = message.attachments?.firstOrNull()

        // A caption wins over the attachment label — it is what distinguishes one media pin
        // from another.
        if (body.isNotEmpty() || attachment == null)
            return formatText(context, from, message)

        return attachmentLabel(context, from, attachment)
    }

    protected open fun attachmentLabel(
        context: Context,
        from: PinnedMessageBodyFormatterAttributes,
        attachment: SceytAttachment,
    ): CharSequence = when (attachment.type) {
        AttachmentTypeEnum.Image.value -> context.getString(R.string.sceyt_photo)
        AttachmentTypeEnum.Video.value -> context.getString(R.string.sceyt_video)
        AttachmentTypeEnum.Voice.value -> context.getString(
            R.string.sceyt_pinned_voice,
            context.getString(R.string.sceyt_voice),
            from.voiceDurationFormatter.format(
                context,
                (attachment.getInfoFromMetadata().duration ?: 0L) * 1000L
            )
        )

        else -> context.getString(R.string.sceyt_file)
    }

    protected open fun formatText(
        context: Context,
        from: PinnedMessageBodyFormatterAttributes,
        message: SceytMessage,
    ): CharSequence {
        val formatted = SceytChatUIKit.formatters.messageBodyFormatter.format(
            context,
            MessageBodyFormatterAttributes(
                message = message,
                mentionTextStyle = from.mentionTextStyle,
                mentionUserNameFormatter = from.mentionUserNameFormatter,
            )
        )
        return formatted.toString().replace(NEW_LINES, " ").trim()
    }

    private companion object {
        val NEW_LINES = Regex("\\s*\\R+\\s*")
    }
}
