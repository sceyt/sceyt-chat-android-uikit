package com.sceyt.chatuikit.persistence.logics.reactionslogic

import com.sceyt.chatuikit.data.messageeventobserver.ReactionUpdateEventData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import kotlinx.coroutines.flow.Flow

interface PersistenceReactionsLogic {
    suspend fun onMessageReactionUpdated(data: ReactionUpdateEventData)
    suspend fun loadReactions(messageId: Long, offset: Int, key: String, loadKey: LoadKeyData?, ignoreDb: Boolean): Flow<PaginationResponse<SceytReaction>>
    suspend fun getMessageReactionsDbByKey(messageId: Long, key: String): List<SceytReaction>
    suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, key: String, isPending: Boolean): SceytResponse<SceytMessage>
    suspend fun sendAllPendingReactions()
}