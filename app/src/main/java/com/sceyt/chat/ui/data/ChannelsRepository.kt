package com.sceyt.chat.ui.data

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChannelsRepository {
    suspend fun getChannels(offset: Int): SceytResponse<List<SceytChannel>>
    suspend fun searchChannels(offset: Int, query: String): SceytResponse<List<SceytChannel>>
    suspend fun leaveChannel(channel: GroupChannel): SceytResponse<Long>
    suspend fun clearHistory(channel: Channel): SceytResponse<Long>
    suspend fun blockChannel(channel: GroupChannel): SceytResponse<Long>
    val onMessageFlow: SharedFlow<Pair<Channel, Message>>
    val onMessageStatusFlow: SharedFlow<MessageStatusChange>
    val onMessageEditedOrDeleteFlow: Flow<Message>
    val onChannelEvenFlow: SharedFlow<ChannelEventData>
}