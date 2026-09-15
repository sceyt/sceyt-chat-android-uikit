package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import kotlinx.coroutines.flow.Flow

internal interface PersistencePinLogic {

    fun getPinnedMessagesFlow(channelId: Long): Flow<List<SceytPinnedMessage>>

    suspend fun getPinnedMessages(channelId: Long): List<SceytPinnedMessage>

    suspend fun pinMessage(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<SceytPinnedMessage>

    suspend fun unpinMessage(channelId: Long, messageTid: Long): SceytResponse<Boolean>

    suspend fun syncChannelPins(channelId: Long)

    suspend fun sendAllPendingPins()

    suspend fun onPinUpdated(event: PinUpdateEvent)

    suspend fun onMessageDeleted(channelId: Long, messageTid: Long)
}
