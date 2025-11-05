package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEvent
import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage

interface PersistencePollLogic {
    suspend fun toggleVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
        optionId: String,
    ): SceytResponse<ChangeVoteResponseData>

    suspend fun retractVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
    ): SceytResponse<ChangeVoteResponseData>

    suspend fun endPoll(
        channelId: Long,
        messageTid: Long,
        pollId: String,
    ): SceytResponse<SceytMessage>

    suspend fun sendAllPendingVotes()

    suspend fun onPollUpdated(event: PollUpdateEvent)
}

