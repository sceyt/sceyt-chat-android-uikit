package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage

interface MessagePollInteractor {
    suspend fun toggleVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
        optionId: String
    ): SceytResponse<ChangeVoteResponseData>

    suspend fun retractVote(
        channelId: Long,
        messageTid: Long,
        pollId: String
    ): SceytResponse<ChangeVoteResponseData>

    suspend fun endPoll(
        channelId: Long,
        messageTid: Long,
        pollId: String
    ): SceytResponse<SceytMessage>

    suspend fun sendAllPendingVotes()
}

