package com.sceyt.chatuikit.data.managers.message.event

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote

sealed class PollUpdateEvent(
    val messageTid: Long
) {

    data class VoteChanged(
        val message: SceytMessage,
        val addedVotes: List<Vote>,
        val removedVotes: List<Vote>,
    ) : PollUpdateEvent(message.tid)

    data class VoteRetracted(
        val message: SceytMessage,
        val retractedVotes: List<Vote>,
    ) : PollUpdateEvent(message.tid)

    data class PollClosed(
        val message: SceytMessage
    ) : PollUpdateEvent(message.tid)
}

