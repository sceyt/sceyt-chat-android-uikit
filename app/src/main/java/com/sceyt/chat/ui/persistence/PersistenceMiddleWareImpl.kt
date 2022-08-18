package com.sceyt.chat.ui.persistence

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventData
import com.sceyt.chat.ui.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.chat.ui.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.chat.ui.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.chat.ui.data.messageeventobserver.MessageEventsObserver
import com.sceyt.chat.ui.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.CreateChannelData
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.persistence.logics.PersistenceChannelLogic
import com.sceyt.chat.ui.persistence.logics.PersistenceMembersLogic
import com.sceyt.chat.ui.persistence.logics.PersistenceMessagesLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


internal class PersistenceMiddleWareImpl(
        private val channelLogic: PersistenceChannelLogic,
        private val messagesLogic: PersistenceMessagesLogic,
        private val membersLogic: PersistenceMembersLogic) : PersistenceChanelMiddleWare,
        PersistenceMembersMiddleWare, PersistenceMessagesMiddleWare, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    init {
        launch { ChannelEventsObserver.onChannelEventFlow.collect(::onChannelEvent) }
        launch { ChannelEventsObserver.onChannelMembersEventFlow.collect(::onChannelMemberEvent) }
        launch { ChannelEventsObserver.onChannelOwnerChangedEventFlow.collect(::onChannelOwnerChangedEvent) }
        launch { ChannelEventsObserver.onMessageStatusFlow.collect(::onMessageStatusChangeEvent) }

        launch { MessageEventsObserver.onMessageFlow.collect(::onMessage) }
        launch { MessageEventsObserver.onMessageReactionUpdatedFlow.collect(::onMessageReactionUpdated) }
        launch { MessageEventsObserver.onMessageEditedOrDeletedFlow.collect(::onMessageEditedOrDeleted) }
    }

    private fun onChannelEvent(data: ChannelEventData) {
        channelLogic.onChannelEvent(data)
    }

    private fun onChannelMemberEvent(data: ChannelMembersEventData) {
        membersLogic.onChannelMemberEvent(data)
    }

    private fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData) {
        membersLogic.onChannelOwnerChangedEvent(data)
    }

    private fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        messagesLogic.onMessageStatusChangeEvent(data)
    }

    private fun onMessage(data: Pair<Channel, Message>) {
        messagesLogic.onMessage(data)
        channelLogic.onMessage(data)
    }

    private fun onMessageReactionUpdated(data: Message?) {
        messagesLogic.onMessageReactionUpdated(data)
    }

    private fun onMessageEditedOrDeleted(data: Message?) {
        messagesLogic.onMessageEditedOrDeleted(data)
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String): Flow<PaginationResponse<SceytChannel>> {
        return channelLogic.loadChannels(offset, searchQuery)
    }

    override suspend fun markChannelAsRead(channel: SceytChannel): SceytResponse<MessageListMarker> {
        return channelLogic.markChannelAsRead(channel)
    }

    override suspend fun clearHistory(channel: SceytChannel): SceytResponse<Long> {
        return channelLogic.clearHistory(channel)
    }

    override suspend fun blockAndLeaveChannel(channel: SceytChannel): SceytResponse<Long> {
        return channelLogic.blockAndLeaveChannel(channel)
    }

    override suspend fun deleteChannel(channel: SceytChannel): SceytResponse<Long> {
        return channelLogic.deleteChannel(channel)
    }

    override suspend fun leaveChannel(channel: SceytChannel): SceytResponse<Long> {
        return channelLogic.leaveChannel(channel)
    }

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        return channelLogic.createDirectChannel(user)
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        return channelLogic.createChannel(createChannelData)
    }

    override suspend fun muteChannel(channel: SceytChannel, muteUntil: Long): SceytResponse<SceytChannel> {
        return channelLogic.muteChannel(channel, muteUntil)
    }

    override suspend fun unMuteChannel(channel: SceytChannel): SceytResponse<SceytChannel> {
        return channelLogic.unMuteChannel(channel)
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.getChannelFromServer(channelId)
    }

    override suspend fun editChannel(channel: SceytChannel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel> {
        return channelLogic.editChannel(channel, newSubject, avatarUrl)
    }

    override suspend fun loadChannelMembers(channelId: Long, offset: Int): Flow<PaginationResponse<SceytMember>> {
        return membersLogic.loadChannelMembers(channelId, offset)
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>> {
        return membersLogic.blockUnBlockUser(userId, block)
    }

    override suspend fun changeChannelOwner(channel: SceytChannel, newOwnerId: String): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelOwner(channel, newOwnerId)
    }

    override suspend fun changeChannelMemberRole(channel: SceytChannel, member: SceytMember): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelMemberRole(channel, member)
    }

    override suspend fun addMembersToChannel(channel: SceytChannel, members: List<Member>): SceytResponse<SceytChannel> {
        return membersLogic.addMembersToChannel(channel, members)
    }

    override suspend fun blockAndDeleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel> {
        return membersLogic.blockAndDeleteMember(channel, memberId)
    }

    override suspend fun deleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel> {
        return membersLogic.deleteMember(channel, memberId)
    }

    override suspend fun loadMessages(channel: SceytChannel, conversationId: Long, lastMessageId: Long, replayInThread: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadMessages(channel, conversationId, lastMessageId, replayInThread)
    }
}