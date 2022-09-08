package com.sceyt.sceytchatuikit.data.repositories

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember

interface ChannelsRepository {
    suspend fun getChannel(id: Long): SceytResponse<SceytChannel>
    suspend fun getChannels(query: String): SceytResponse<List<SceytChannel>>
    suspend fun loadMoreChannels(): SceytResponse<List<SceytChannel>>
    suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel>
    suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun leaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun clearHistory(channelId: Long): SceytResponse<Long>
    suspend fun markAsRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun blockUser(userId: String): SceytResponse<List<User>>
    suspend fun unblockUser(userId: String): SceytResponse<List<User>>
    suspend fun blockChannel(channelId: Long): SceytResponse<Long>
    suspend fun unBlockChannel(channelId: Long): SceytResponse<Long>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
    suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel>
    suspend fun deleteChannel(channelId: Long): SceytResponse<Long>
    suspend fun loadChannelMembers(channelId: Long, offset: Int): SceytResponse<List<SceytMember>>
    suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun changeChannelOwner(channelId: Long, userId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channelId: Long, member: Member): SceytResponse<SceytChannel>
    suspend fun deleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channelId: Long, userId: String): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel>
}