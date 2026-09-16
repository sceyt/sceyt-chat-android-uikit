package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import kotlinx.coroutines.flow.Flow

interface MessagePinInteractor {

    /** The channel's pins, in pin order, updating as they change. */
    fun getPinnedMessagesFlow(channelId: Long): Flow<List<SceytPinnedMessage>>

    suspend fun getPinnedMessages(channelId: Long): List<SceytPinnedMessage>

    suspend fun pinMessage(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<SceytPinnedMessage>

    suspend fun unpinMessage(
        channelId: Long,
        messageTid: Long,
    ): SceytResponse<Boolean>

    /**
     * Sends any pending intents for the channel, then reloads the server's pins and
     * reconciles the local table against them. Called when a channel is opened.
     */
    suspend fun syncChannelPins(channelId: Long)

    suspend fun sendAllPendingPins()
}