package com.sceyt.sceytchatuikit.persistence

import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import kotlinx.coroutines.flow.Flow

interface PersistenceReactionsMiddleWare {
    suspend fun loadReactions(messageId: Long, offset: Int, key: String, loadKey: LoadKeyData?, ignoreDb: Boolean): Flow<PaginationResponse<SceytReaction>>
    suspend fun getMessageReactionsDbByKey(messageId: Long, key: String): List<SceytReaction>
    suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String, isPending: Boolean): SceytResponse<SceytMessage>
}