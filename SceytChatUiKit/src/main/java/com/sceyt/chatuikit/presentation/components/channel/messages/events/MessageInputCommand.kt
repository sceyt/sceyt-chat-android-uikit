package com.sceyt.chatuikit.presentation.components.channel.messages.events

import com.sceyt.chatuikit.data.models.messages.SceytMessage

/**
 * One-shot commands sent from the view model to the message input (consumed in InputViewBinding).
 */
sealed interface MessageInputCommand {
    data class Edit(val message: SceytMessage) : MessageInputCommand
    data class Reply(val message: SceytMessage) : MessageInputCommand
}