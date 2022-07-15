package com.sceyt.chat.ui.data

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.chat.ui.data.channeleventobserver.MessageStatusChange
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.CreateChannelData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChannelsRepository {
    suspend fun getChannel(id: Long): SceytResponse<SceytChannel>
    suspend fun getChannels(query: String): SceytResponse<List<SceytChannel>>
    suspend fun loadMoreChannels(): SceytResponse<List<SceytChannel>>
    suspend fun createChannel(channelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun leaveChannel(channel: GroupChannel): SceytResponse<Long>
    suspend fun clearHistory(channel: Channel): SceytResponse<Long>
    suspend fun markAsRead(channel: Channel): SceytResponse<MessageListMarker>
    suspend fun blockUser(userId: String): SceytResponse<List<User>>
    suspend fun unblockUser(userId: String): SceytResponse<List<User>>
    suspend fun blockChannel(channel: GroupChannel): SceytResponse<Long>
    suspend fun uploadAvatar(avatarUri: String): SceytResponse<String>
    suspend fun editChannel(channel: Channel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel>
    suspend fun deleteChannel(channel: Channel): SceytResponse<Long>
    suspend fun loadChannelMembers(channelId: Long): SceytResponse<List<SceytMember>>
    suspend fun addMembersToChannel(channel: GroupChannel, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun changeChannelOwner(channel: GroupChannel, userId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channel: GroupChannel, member: Member): SceytResponse<SceytChannel>
    suspend fun deleteMember(channel: GroupChannel, userId: String): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channel: GroupChannel, userId: String): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channel: Channel): SceytResponse<SceytChannel>
    suspend fun muteChannel(channel: Channel, muteUntil: Long): SceytResponse<SceytChannel>
    val onMessageFlow: SharedFlow<Pair<Channel, Message>>
    val onMessageStatusFlow: SharedFlow<MessageStatusChange>
    val onMessageEditedOrDeleteFlow: Flow<Message>
    val onChannelEvenFlow: SharedFlow<ChannelEventData>
    val onChannelTypingEvenFlow: SharedFlow<ChannelTypingEventData>
    val onOutGoingMessageFlow: Flow<SceytMessage>
}