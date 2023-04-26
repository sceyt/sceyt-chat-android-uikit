package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import kotlinx.coroutines.flow.Flow

interface PersistenceChanelMiddleWare {
    suspend fun loadChannels(offset: Int, searchQuery: String, loadKey: LoadKeyData?,
                             ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>>

    suspend fun searchChannels(offset: Int, limit: Int, searchItems: List<String>, loadKey: LoadKeyData?,
                               onlyMine: Boolean, ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>>

    suspend fun syncChannels(limit: Int): Flow<SceytResponse<List<SceytChannel>>>
    suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long>
    suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun deleteChannel(channelId: Long): SceytResponse<Long>
    suspend fun leaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel>
    suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromDb(channelId: Long): SceytChannel?
    suspend fun getDirectChannelFromDb(peerId: String): SceytChannel?
    suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromServerByUrl(uri: String): SceytResponse<List<SceytChannel>>
    suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel>
    suspend fun join(channelId: Long): SceytResponse<SceytChannel>
    suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun updateDraftMessage(channelId: Long, message: String?, mentionUsers: List<Mention>)
    fun getTotalUnreadCount(): Flow<Int>
}