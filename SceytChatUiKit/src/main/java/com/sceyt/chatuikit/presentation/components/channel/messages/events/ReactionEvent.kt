package com.sceyt.chatuikit.presentation.components.channel.messages.events

import com.sceyt.chatuikit.data.models.messages.SceytMessage

sealed class ReactionEvent {

    data class AddReaction(
            val message: SceytMessage,
            val scoreKey: String
    ) : ReactionEvent()

    data class RemoveReaction(
            val message: SceytMessage,
            val scoreKey: String
    ) : ReactionEvent()
}