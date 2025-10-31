package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote

data class PollUpdateEventData(
        val message: SceytMessage,
        val votes: List<Vote>?,
        val eventType: PollUpdateEventEnum
)

enum class PollUpdateEventEnum {
    VoteAdded,
    VoteDeleted,
    VoteRetracted,
    PollClosed
}

