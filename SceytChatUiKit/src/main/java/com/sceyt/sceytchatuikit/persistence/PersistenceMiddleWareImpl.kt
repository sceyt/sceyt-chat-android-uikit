package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.Types
import com.sceyt.chat.models.Status
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogic
import com.sceyt.sceytchatuikit.persistence.logics.memberslogic.PersistenceMembersLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.logics.userslogic.PersistenceUsersLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class PersistenceMiddleWareImpl(private val channelLogic: PersistenceChannelsLogic,
                                         private val messagesLogic: PersistenceMessagesLogic,
                                         private val membersLogic: PersistenceMembersLogic,
                                         private val usersLogic: PersistenceUsersLogic,
                                         private val connectionLogic: PersistenceConnectionLogic) :
        CoroutineScope, PersistenceMembersMiddleWare, PersistenceMessagesMiddleWare,
        PersistenceChanelMiddleWare, PersistenceUsersMiddleWare, SceytKoinComponent {


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    init {
        // Channel events
        launch { ChannelEventsObserver.onChannelEventFlow.collect(::onChannelEvent) }
        launch { ChannelEventsObserver.onChannelMembersEventFlow.collect(::onChannelMemberEvent) }
        launch { ChannelEventsObserver.onChannelOwnerChangedEventFlow.collect(::onChannelOwnerChangedEvent) }
        // Message events
        launch { ChannelEventsObserver.onMessageStatusFlow.collect(::onMessageStatusChangeEvent) }
        launch { MessageEventsObserver.onMessageFlow.collect(::onMessage) }
        launch { MessageEventsObserver.onMessageReactionUpdatedFlow.collect(::onMessageReactionUpdated) }
        launch { MessageEventsObserver.onMessageEditedOrDeletedFlow.collect(::onMessageEditedOrDeleted) }

        // Connection events
        launch { ConnectionObserver.onChangedConnectStatusFlow.collect(::onChangedConnectStatus) }
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

    private fun onChangedConnectStatus(data: Pair<Types.ConnectState, Status?>) {
        connectionLogic.onChangedConnectStatus(data.first, data.second)
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String): Flow<PaginationResponse<SceytChannel>> {
        return channelLogic.loadChannels(offset, searchQuery)
    }

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.markChannelAsRead(channelId)
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.markChannelAsUnRead(channelId)
    }

    override suspend fun clearHistory(channelId: Long): SceytResponse<Long> {
        return channelLogic.clearHistory(channelId)
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.blockAndLeaveChannel(channelId)
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.deleteChannel(channelId)
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.leaveChannel(channelId)
    }

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        return channelLogic.createDirectChannel(user)
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        return channelLogic.createChannel(createChannelData)
    }

    override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> {
        return channelLogic.muteChannel(channelId, muteUntil)
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.unMuteChannel(channelId)
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.getChannelFromServer(channelId)
    }

    override suspend fun getChannelFromServerByUrl(uri: String): SceytResponse<List<SceytChannel>> {
        return channelLogic.getChannelFromServerByUrl(uri)
    }

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        return channelLogic.editChannel(channelId, data)
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.join(channelId)
    }

    override suspend fun loadChannelMembers(channelId: Long, offset: Int): Flow<PaginationResponse<SceytMember>> {
        return membersLogic.loadChannelMembers(channelId, offset)
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>> {
        return membersLogic.blockUnBlockUser(userId, block)
    }

    override suspend fun changeChannelOwner(channelId: Long, newOwnerId: String): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelOwner(channelId, newOwnerId)
    }

    override suspend fun changeChannelMemberRole(channelId: Long, member: SceytMember): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelMemberRole(channelId, member)
    }

    override suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel> {
        return membersLogic.addMembersToChannel(channelId, members)
    }

    override suspend fun blockAndDeleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel> {
        return membersLogic.blockAndDeleteMember(channelId, memberId)
    }

    override suspend fun deleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel> {
        return membersLogic.deleteMember(channelId, memberId)
    }

    override suspend fun loadMessages(conversationId: Long,
                                      lastMessageId: Long,
                                      replayInThread: Boolean, offset: Int): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadMessages(conversationId, lastMessageId, replayInThread, offset)
    }

    override suspend fun sendMessage(channelId: Long, message: Message, tmpMessageCb: (Message) -> Unit): SceytResponse<SceytMessage?> {
        return messagesLogic.sendMessage(channelId, message, tmpMessageCb)
    }

    override suspend fun sendPendingMessages(channelId: Long) {
        return messagesLogic.sendPendingMessages(channelId)
    }

    override suspend fun deleteMessage(channelId: Long, messageId: Long, messageTid: Long): SceytResponse<SceytMessage> {
        return messagesLogic.deleteMessage(channelId, messageId, messageTid)
    }

    override suspend fun markAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        return messagesLogic.markAsRead(channelId, *ids)
    }

    override suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        return messagesLogic.editMessage(channelId, message)
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return messagesLogic.addReaction(channelId, messageId, scoreKey)
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return messagesLogic.deleteReaction(channelId, messageId, scoreKey)
    }

    override suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<User>> {
        return usersLogic.getSceytUsers(ids)
    }

    override suspend fun getCurrentUser(): User? {
        return usersLogic.getCurrentUser()
    }
}