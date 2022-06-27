package com.sceyt.chat.ui.data

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.ui.data.channeleventobserverservice.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserverservice.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChannelsRepository {
    suspend fun getChannel(id: Long): SceytResponse<SceytChannel>
    suspend fun getChannels(offset: Int): SceytResponse<List<SceytChannel>>
    suspend fun searchChannels(offset: Int, query: String): SceytResponse<List<SceytChannel>>
    suspend fun leaveChannel(channel: GroupChannel): SceytResponse<Long>
    suspend fun clearHistory(channel: Channel): SceytResponse<Long>
    suspend fun blockChannel(channel: GroupChannel): SceytResponse<Long>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
    suspend fun editChannel(channel: Channel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel>
    suspend fun deleteChannel(channel: Channel): SceytResponse<Long>
    suspend fun loadChannelMembers(channelId: Long): SceytResponse<List<SceytMember>>
    suspend fun addMembersToChannel(channel: GroupChannel, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun changeChannelOwner(channel: GroupChannel, userId: String): SceytResponse<String>
    suspend fun changeChannelMemberRole(channel: GroupChannel, member: Member): SceytResponse<SceytChannel>
    val onMessageFlow: SharedFlow<Pair<Channel, Message>>
    val onMessageStatusFlow: SharedFlow<MessageStatusChange>
    val onMessageEditedOrDeleteFlow: Flow<Message>
    val onChannelEvenFlow: SharedFlow<ChannelEventData>
}