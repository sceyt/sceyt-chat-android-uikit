package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage

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
}

