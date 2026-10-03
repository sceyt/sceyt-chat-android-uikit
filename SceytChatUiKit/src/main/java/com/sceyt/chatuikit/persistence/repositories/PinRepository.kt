package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import kotlinx.coroutines.flow.Flow

internal interface PinRepository {

    suspend fun pinMessages(
        channelId: Long,
        messageIds: List<Long>,
        pinType: PinType,
        pinTill: Long?,
    ): SceytResponse<List<SceytPinnedMessage>>

    suspend fun unpinMessages(
        channelId: Long,
        messageIds: List<Long>,
    ): SceytResponse<List<SceytPinnedMessage>>

    fun getPinnedMessages(channelId: Long): Flow<SceytPagingResponse<List<SceytPinnedMessage>>>
}