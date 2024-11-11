package com.sceyt.chatuikit.persistence.repositories

import com.sceyt.chat.models.member.Member
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import kotlinx.coroutines.flow.Flow

interface ChannelsRepository {
    suspend fun getChannel(id: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromServerByUrl(url: String): SceytResponse<List<SceytChannel>>
    suspend fun getChannels(query: String, config: ChannelListConfig): SceytResponse<List<SceytChannel>>
    suspend fun getAllChannels(limit: Int): Flow<GetAllChannelsResponse>
    suspend fun loadMoreChannels(): SceytResponse<List<SceytChannel>>
    suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun leaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long>
    suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun unHideChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun blockChannel(channelId: Long): SceytResponse<Long>
    suspend fun unBlockChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
    suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel>
    suspend fun deleteChannel(channelId: Long): SceytResponse<Long>
    suspend fun loadChannelMembers(channelId: Long, offset: Int, role: String?): SceytResponse<List<SceytMember>>
    suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun changeChannelOwner(channelId: Long, userId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channelId: Long, vararg member: Member): SceytResponse<SceytChannel>
    suspend fun deleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel>
    suspend fun enableAutoDelete(channelId: Long, period: Long): SceytResponse<SceytChannel>
    suspend fun disableAutoDelete(channelId: Long): SceytResponse<SceytChannel>
    suspend fun pinChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun unpinChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun join(channelId: Long): SceytResponse<SceytChannel>
}