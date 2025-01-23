package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import kotlinx.coroutines.flow.Flow

interface MessageReactionInteractor {
    suspend fun loadReactions(
            messageId: Long,
            offset: Int,
            key: String,
            loadKey: LoadKeyData?,
            ignoreDb: Boolean
    ): Flow<PaginationResponse<SceytReaction>>

    suspend fun getLocalMessageReactionsById(reactionId: Long): SceytReaction?
    suspend fun getLocalMessageReactionsByKey(messageId: Long, key: String): List<SceytReaction>
    suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int,
                            reason: String, enforceUnique: Boolean): SceytResponse<SceytMessage>

    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
}