package com.sceyt.chat.ui.data.repositories

import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.CreateChannelData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytGroupChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember

interface ChannelsRepository {
    suspend fun getChannel(id: Long): SceytResponse<SceytChannel>
    suspend fun getChannels(query: String): SceytResponse<List<SceytChannel>>
    suspend fun loadMoreChannels(): SceytResponse<List<SceytChannel>>
    suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel>
    suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun leaveChannel(channel: SceytGroupChannel): SceytResponse<Long>
    suspend fun clearHistory(channel: SceytChannel): SceytResponse<Long>
    suspend fun markAsRead(channel: SceytChannel): SceytResponse<MessageListMarker>
    suspend fun blockUser(userId: String): SceytResponse<List<User>>
    suspend fun unblockUser(userId: String): SceytResponse<List<User>>
    suspend fun blockChannel(channel: SceytGroupChannel): SceytResponse<Long>
    suspend fun unBlockChannel(channel: SceytGroupChannel): SceytResponse<Long>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
    suspend fun editChannel(channel: SceytGroupChannel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel>
    suspend fun deleteChannel(channel: SceytChannel): SceytResponse<Long>
    suspend fun loadChannelMembers(channelId: Long, offset: Int): SceytResponse<List<SceytMember>>
    suspend fun addMembersToChannel(channel: SceytGroupChannel, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun changeChannelOwner(channel: SceytGroupChannel, userId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channel: SceytGroupChannel, member: Member): SceytResponse<SceytChannel>
    suspend fun deleteMember(channel: SceytGroupChannel, userId: String): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channel: SceytGroupChannel, userId: String): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channel: SceytChannel): SceytResponse<SceytChannel>
    suspend fun muteChannel(channel: SceytChannel, muteUntil: Long): SceytResponse<SceytChannel>
}