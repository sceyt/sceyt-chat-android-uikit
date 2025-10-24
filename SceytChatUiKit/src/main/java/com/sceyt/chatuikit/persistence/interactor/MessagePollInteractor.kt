package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage

interface MessagePollInteractor {
    suspend fun toggleVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
        optionId: String
    ): SceytResponse<SceytMessage>

    suspend fun sendAllPendingVotes()
}

