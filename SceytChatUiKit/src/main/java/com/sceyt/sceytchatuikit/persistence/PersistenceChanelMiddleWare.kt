package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import kotlinx.coroutines.flow.Flow

interface PersistenceChanelMiddleWare {
    suspend fun loadChannels(offset: Int, searchQuery: String): Flow<PaginationResponse<SceytChannel>>
    suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun clearHistory(channelId: Long): SceytResponse<Long>
    suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun deleteChannel(channelId: Long): SceytResponse<Long>
    suspend fun leaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel>
    suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromServerByUrl(uri: String): SceytResponse<List<SceytChannel>>
    suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel>
    suspend fun join(channelId: Long): SceytResponse<SceytChannel>
}