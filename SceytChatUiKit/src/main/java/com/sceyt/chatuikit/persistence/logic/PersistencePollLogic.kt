package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage

interface PersistencePollLogic {
    suspend fun toggleVote(
            channelId: Long,
            messageTid: Long,
            pollId: String,
            optionId: String,
    ): SceytResponse<SceytMessage>

    suspend fun retractVote(
            channelId: Long,
            messageTid: Long,
            pollId: String,
    ): SceytResponse<SceytMessage>

    suspend fun endPoll(
            channelId: Long,
            messageTid: Long,
            pollId: String,
    ): SceytResponse<SceytMessage>

    suspend fun sendAllPendingVotes()

    suspend fun onPollUpdated(message: SceytMessage)
}

