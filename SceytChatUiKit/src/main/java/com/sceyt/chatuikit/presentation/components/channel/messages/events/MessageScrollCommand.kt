package com.sceyt.chatuikit.presentation.components.channel.messages.events

/**
 * One-shot commands sent from the view model to the messages list.
 *
 * Only the latest pending scroll command is relevant.
 */
sealed interface MessageScrollCommand {
    data class ToLastMessage(val messageId: Long) : MessageScrollCommand
    data class ToReplyMessage(val messageId: Long) : MessageScrollCommand
    data class ToSearchMessage(val messageId: Long) : MessageScrollCommand
    data class ToUnreadMention(val messageId: Long) : MessageScrollCommand
}
