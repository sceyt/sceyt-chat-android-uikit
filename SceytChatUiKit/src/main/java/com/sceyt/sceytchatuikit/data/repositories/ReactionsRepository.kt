package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage

interface ReactionsRepository {
    suspend fun getReactions(messageId: Long, key: String): SceytResponse<List<Reaction>>
    suspend fun loadMoreReactions(messageId: Long, key: String): SceytResponse<List<Reaction>>
    suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
    suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage>
}