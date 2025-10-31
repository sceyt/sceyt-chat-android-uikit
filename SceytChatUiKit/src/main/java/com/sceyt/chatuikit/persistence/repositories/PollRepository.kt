package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage

interface PollRepository {
    suspend fun addVote(
            channelId: Long,
            messageId: Long,
            pollId: String,
            optionId: String,
    ): SceytResponse<SceytMessage>

    suspend fun deleteVote(
            channelId: Long,
            messageId: Long,
            pollId: String,
            optionId: String,
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

