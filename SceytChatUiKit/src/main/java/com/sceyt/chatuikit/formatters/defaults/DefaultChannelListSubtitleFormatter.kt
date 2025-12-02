package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.extensions.toSpannableString
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.ChannelItemSubtitleFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.DraftMessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChatReactionMessagesCache
import com.sceyt.chatuikit.persistence.mappers.toSceytAttachment
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.presentation.extensions.isSupportedType

open class DefaultChannelListSubtitleFormatter : Formatter<ChannelItemSubtitleFormatterAttributes> {
    override fun format(
        context: Context,
        from: ChannelItemSubtitleFormatterAttributes
    ): CharSequence {
        val channel = from.channel
        val (hasLastReaction, reactionTitle) = checkHasLastReaction(context, from)
        if (hasLastReaction)
            return reactionTitle

        val (hasDraft, draftMessage) = checkHasDraftMessage(context, from)
        if (hasDraft)
            return draftMessage

        val style = from.channelItemStyle
        val message = channel.lastMessage ?: return ""

        if (message.state == MessageState.Deleted) {
            val text = SpannableStringBuilder(style.messageDeletedStateText)
            style.deletedTextStyle.apply(context, text)
            return text
        }

        if (message.type == SceytMessageType.System.value) {
            return SceytChatUIKit.formatters.systemMessageBodyFormatter.format(context, message)
        }

        if (!message.isSupportedType()) {
            return SceytChatUIKit.formatters.unsupportedMessageShortBodyFormatter.format(
                context = context,
                from = message
            )
        }

        val body = style.lastMessageBodyFormatter.format(
            context, MessageBodyFormatterAttributes(
                message = message,
                mentionTextStyle = style.mentionTextStyle
            )
        )

        if (message.type == SceytMessageType.System.value) {
            return body
        }

        val senderName = style.lastMessageSenderNameFormatter.format(context, channel)
        val attachmentIcon = message.attachments?.firstOrNull()?.let {
            style.attachmentIconProvider.provide(context, it)
        }

        return buildSpannedString {
            if (senderName.isNotEmpty()) {
                append(senderName)
                style.lastMessageSenderNameTextStyle.apply(context, this, 0, senderName.length)
            }
            append(attachmentIcon.toSpannableString())
            append(body)
        }
    }

    open fun checkHasLastReaction(
        context: Context,
        from: ChannelItemSubtitleFormatterAttributes,
    ): Pair<Boolean, CharSequence> {
        val channel = from.channel
        val style = from.channelItemStyle
        val myId = SceytChatUIKit.chatUIFacade.myId
        if (channel.lastMessage?.deliveryStatus == DeliveryStatus.Pending) return false to ""
        val pendingAddOrRemoveReaction = channel.pendingReactions?.filter {
            !it.incomingMsg
        }?.groupBy { it.isAdd }
        val addReactions = pendingAddOrRemoveReaction?.get(true)
        val removeReactions = pendingAddOrRemoveReaction?.get(false) ?: emptyList()
        val lastReaction = addReactions?.maxByOrNull { it.createdAt }?.toSceytReaction()
            ?: channel.newReactions?.filter {
                it.user?.id != myId && removeReactions.none { rm ->
                    rm.key == it.key && rm.messageId == it.messageId && it.user?.id == myId
                }
            }?.maxByOrNull { it.id } ?: return false to ""

        val message = ChatReactionMessagesCache.getMessageById(lastReaction.messageId)
            ?: return false to ""

        if (lastReaction.id > (channel.lastMessage?.id ?: 0) || lastReaction.pending) {
            val body = style.lastMessageBodyFormatter.format(
                context = context,
                from = MessageBodyFormatterAttributes(
                    message = message,
                    mentionTextStyle = style.mentionTextStyle
                )
            )

            val attachmentIcon = message.attachments?.firstOrNull()?.let {
                style.attachmentIconProvider.provide(context, it)
            }

            val toMessage = SpannableStringBuilder(attachmentIcon.toSpannableString())
            toMessage.append(body)

            val reactedWord = context.getString(R.string.sceyt_reacted)

            val reactUserName = when {
                channel.isGroup -> {
                    val name = lastReaction.user?.let {
                        style.reactedUserNameFormatter.format(
                            context,
                            it
                        )
                    }
                        ?: ""
                    "$name ${reactedWord.lowercase()}"
                }

                lastReaction.user?.id == myId -> "${context.getString(R.string.sceyt_you)} ${
                    context.getString(
                        R.string.sceyt_reacted
                    ).lowercase()
                }"

                else -> context.getString(R.string.sceyt_reacted)
            }

            val text = "$reactUserName ${lastReaction.key} ${context.getString(R.string.sceyt_to)}"
            val title = SpannableStringBuilder("$text ")
            title.append("\"")
            title.append(toMessage)
            title.append("\"")
            return true to title
        }
        return false to ""
    }

    open fun checkHasDraftMessage(
        context: Context,
        attributes: ChannelItemSubtitleFormatterAttributes,
    ): Pair<Boolean, CharSequence> {
        val channel = attributes.channel
        val style = attributes.channelItemStyle
        val draftMessage = channel.draftMessage
        return if (draftMessage != null) {
            val draft = "${context.getString(R.string.sceyt_draft)}:".toSpannable()
            style.draftPrefixTextStyle.apply(context, draft)

            val formattedBody = style.draftMessageBodyFormatter.format(
                context, DraftMessageBodyFormatterAttributes(
                    message = draftMessage,
                    mentionTextStyle = style.mentionTextStyle
                )
            )

            if (formattedBody.isBlank()) {
                return true to draft.removeSuffix(":").toSpannable()
            }

            val attachment = draftMessage.voiceAttachment?.toSceytAttachment()
                ?: draftMessage.attachments?.singleOrNull()?.toSceytAttachment()

            val attachmentIcon = attachment?.let {
                style.attachmentIconProvider.provide(context, it)
            }

            val body = buildSpannedString {
                append(draft)
                append(" ")
                append(attachmentIcon.toSpannableString())
                append(formattedBody)
            }
            true to body
        } else false to ""
    }
}