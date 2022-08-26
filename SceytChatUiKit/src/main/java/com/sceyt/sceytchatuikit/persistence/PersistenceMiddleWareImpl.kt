package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.logics.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.logics.PersistenceMembersLogic
import com.sceyt.sceytchatuikit.persistence.logics.PersistenceMessagesLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


internal class PersistenceMiddleWareImpl(
        private val channelLogic: PersistenceChannelsLogic,
        private val messagesLogic: PersistenceMessagesLogic,
        private val membersLogic: PersistenceMembersLogic) : CoroutineScope,
        com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare, com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare, com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    init {
        // Channel events
        launch { com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelEventFlow.collect(::onChannelEvent) }
        launch { com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelMembersEventFlow.collect(::onChannelMemberEvent) }
        launch { com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onChannelOwnerChangedEventFlow.collect(::onChannelOwnerChangedEvent) }
        // Message events
        launch { com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver.onMessageStatusFlow.collect(::onMessageStatusChangeEvent) }
        launch { MessageEventsObserver.onMessageFlow.collect(::onMessage) }
        launch { MessageEventsObserver.onMessageReactionUpdatedFlow.collect(::onMessageReactionUpdated) }
        launch { MessageEventsObserver.onMessageEditedOrDeletedFlow.collect(::onMessageEditedOrDeleted) }
    }

    private fun onChannelEvent(data: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData) {
        channelLogic.onChannelEvent(data)
    }

    private fun onChannelMemberEvent(data: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData) {
        membersLogic.onChannelMemberEvent(data)
    }

    private fun onChannelOwnerChangedEvent(data: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData) {
        membersLogic.onChannelOwnerChangedEvent(data)
    }

    private suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        messagesLogic.onMessageStatusChangeEvent(data)
    }

    private suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
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

    override suspend fun editChannel(channel: SceytGroupChannel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel> {
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

    override suspend fun loadMessages(channel: SceytChannel,
                                      conversationId: Long,
                                      lastMessageId: Long,
                                      replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadMessages(channel, conversationId, lastMessageId, replayInThread, offset)
    }

    override suspend fun sendMessage(channel: SceytChannel, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        return messagesLogic.sendMessage(channel, message, tmpMessageCb)
    }

    override suspend fun deleteMessage(channel: SceytChannel, messageId: Long): SceytResponse<SceytMessage> {
        return messagesLogic.deleteMessage(channel, messageId)
    }
}