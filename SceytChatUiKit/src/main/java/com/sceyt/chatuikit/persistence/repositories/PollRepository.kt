package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote

interface PollRepository {
    suspend fun addVotes(
        channelId: Long,
        messageId: Long,
        pollId: String,
        optionIds: List<String>
    ): SceytResponse<SceytMessage>

    suspend fun deleteVotes(
        channelId: Long,
        messageId: Long,
        pollId: String,
        optionIds: List<String>
    ): SceytResponse<SceytMessage>

    suspend fun retractVote(
        channelId: Long,
        messageId: Long,
        pollId: String,
    ): SceytResponse<SceytMessage>

    suspend fun endPoll(
        channelId: Long,
        messageId: Long,
        pollId: String,
    ): SceytResponse<SceytMessage>

    suspend fun getPollVotes(
        pollId: String,
        optionId: String,
        nextToken: String
    ): SceytPagingResponse<List<Vote>>

}

