package com.sceyt.sceytchatuikit.persistence.logics

import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow

internal interface PersistenceChannelsLogic {
    fun onChannelEvent(data: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData)
    fun onMessage(data: Pair<SceytChannel, SceytMessage>)
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
    suspend fun editChannel(channel: SceytGroupChannel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel>
}