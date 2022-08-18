package com.sceyt.chat.ui.persistence.logics

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventData
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.CreateChannelData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import kotlinx.coroutines.flow.Flow

internal interface PersistenceChannelLogic {
    fun onChannelEvent(data: ChannelEventData)
    fun onMessage(data: Pair<Channel, Message>)
    suspend fun loadChannels(offset: Int, searchQuery: String): Flow<PaginationResponse<SceytChannel>>
    suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel>
    suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun markChannelAsRead(channel: SceytChannel): SceytResponse<MessageListMarker>
    suspend fun clearHistory(channel: SceytChannel): SceytResponse<Long>
    suspend fun blockAndLeaveChannel(channel: SceytChannel): SceytResponse<Long>
    suspend fun leaveChannel(channel: SceytChannel): SceytResponse<Long>
    suspend fun deleteChannel(channel: SceytChannel): SceytResponse<Long>
    suspend fun muteChannel(channel: SceytChannel, muteUntil: Long): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channel: SceytChannel): SceytResponse<SceytChannel>
    suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel>
    suspend fun editChannel(channel: SceytChannel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel>
}