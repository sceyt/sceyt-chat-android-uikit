package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction

interface ReactionsRepository {
    suspend fun getReactions(messageId: Long, key: String): SceytResponse<List<SceytReaction>>
    suspend fun loadMoreReactions(messageId: Long, key: String): SceytResponse<List<SceytReaction>>
    suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
}