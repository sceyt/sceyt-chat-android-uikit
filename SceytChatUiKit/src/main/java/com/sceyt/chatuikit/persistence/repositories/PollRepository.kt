package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote

interface PollRepository {
    suspend fun changeVotes(
        channelId: Long,
        messageId: Long,
        pollId: String,
        addOptionIds: List<String>,
        removeOptionIds: List<String>
    ): SceytResponse<ChangeVoteResponseData>

    suspend fun retractVote(
        channelId: Long,
        messageId: Long,
        pollId: String,
    ): SceytResponse<ChangeVoteResponseData>

    suspend fun endPoll(
        channelId: Long,
        messageId: Long,
        pollId: String,
    ): SceytResponse<SceytMessage>

    suspend fun getPollVotes(
        messageId: Long,
        pollId: String,
        optionId: String,
        nextToken: String
    ): SceytPagingResponse<List<Vote>>

}

