package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.Vote

interface PollRepository {
    suspend fun addVote(pollId: String, optionId: String): SceytResponse<SceytMessage>
    suspend fun deleteVote(pollId: String, optionId: String): SceytResponse<SceytMessage>
    suspend fun getPollVotes(pollId: String, optionId: String, nextToken: String): SceytPagingResponse<List<Vote>>
}

