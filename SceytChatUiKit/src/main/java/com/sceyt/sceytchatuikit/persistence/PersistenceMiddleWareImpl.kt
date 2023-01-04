package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.settings.Settings
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.*
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionStateData
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
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
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        ChannelEventsObserver.onChannelEventFlow.onEach(::onChannelEvent).launchIn(this)
        ChannelEventsObserver.onTotalUnreadChangedFlow.onEach(::onChannelUnreadCountUpdatedEvent).launchIn(this)
        ChannelEventsObserver.onChannelMembersEventFlow.onEach(::onChannelMemberEvent).launchIn(this)
        ChannelEventsObserver.onChannelOwnerChangedEventFlow.onEach(::onChannelOwnerChangedEvent).launchIn(this)
        // Message events
        ChannelEventsObserver.onMessageStatusFlow.onEach(::onMessageStatusChangeEvent).launchIn(this)
        MessageEventsObserver.onMessageFlow.onEach(::onMessage).launchIn(this)
        MessageEventsObserver.onMessageReactionUpdatedFlow.onEach(::onMessageReactionUpdated).launchIn(this)
        MessageEventsObserver.onMessageEditedOrDeletedFlow.onEach(::onMessageEditedOrDeleted).launchIn(this)

        // Connection events
        ConnectionEventsObserver.onChangedConnectStatusFlow.onEach(::onChangedConnectStatus).launchIn(this)
    }


    private fun onChannelEvent(data: ChannelEventData) {
        launch { channelLogic.onChannelEvent(data) }
    }

    private fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData) {
        launch { channelLogic.onChannelUnreadCountUpdatedEvent(data) }
    }

    private fun onChannelMemberEvent(data: ChannelMembersEventData) {
        launch { membersLogic.onChannelMemberEvent(data) }
    }

    private fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData) {
        launch { membersLogic.onChannelOwnerChangedEvent(data) }
    }

    private fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        launch {
            messagesLogic.onMessageStatusChangeEvent(data)
            channelLogic.onMessageStatusChangeEvent(data)
        }
    }

    private fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        launch {
            messagesLogic.onMessage(data)
            channelLogic.onMessage(data)
        }
    }

    private fun onMessageReactionUpdated(data: Message?) {
        data ?: return
        launch { messagesLogic.onMessageReactionUpdated(data) }
    }

    private fun onMessageEditedOrDeleted(data: Message?) {
        data ?: return
        launch {
            val sceytMessage = data.toSceytUiMessage()
            messagesLogic.onMessageEditedOrDeleted(sceytMessage)
            channelLogic.onMessageEditedOrDeleted(sceytMessage)
        }
    }

    private fun onChangedConnectStatus(data: ConnectionStateData) {
        launch { connectionLogic.onChangedConnectStatus(data) }
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String, loadKey: LoadKeyData?,
                                      ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return channelLogic.loadChannels(offset, searchQuery, loadKey, ignoreDb)
    }

    override suspend fun syncChannels(limit: Int): Flow<SceytResponse<List<SceytChannel>>> {
        return channelLogic.syncChannels(limit)
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

    override suspend fun getChannelFromDb(channelId: Long): SceytChannel? {
        return channelLogic.getChannelFromDb(channelId)
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

    override fun getTotalUnreadCount(): Flow<Int> {
        return channelLogic.getTotalUnreadCount()
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

    override suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                          loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadPrevMessages(conversationId, lastMessageId, replyInThread, offset, loadKey, ignoreDb)
    }

    override suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                          offset: Int, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNextMessages(conversationId, lastMessageId, replyInThread, offset, ignoreDb)
    }

    override suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                          loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNearMessages(conversationId, messageId, replyInThread, loadKey, ignoreDb)
    }

    override suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, loadKey: LoadKeyData,
                                            ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNewestMessages(conversationId, replyInThread, loadKey, ignoreDb)
    }

    override suspend fun syncMessagesAfterMessageId(conversationId: Long, replyInThread: Boolean,
                                                    messageId: Long): Flow<SceytResponse<List<SceytMessage>>> {
        return messagesLogic.syncMessagesAfterMessageId(conversationId, replyInThread, messageId)
    }

    override suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult> {
        return messagesLogic.sendMessageAsFlow(channelId, message)
    }

    override suspend fun sendMessage(channelId: Long, message: Message) {
        return messagesLogic.sendMessage(channelId, message)
    }

    override suspend fun sendMessages(channelId: Long, messages: List<Message>) {
        return messagesLogic.sendMessages(channelId, messages)
    }

    override suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message) {
        return messagesLogic.sendMessageWithUploadedAttachments(channelId, message)
    }

    override suspend fun sendPendingMessages(channelId: Long) {
        return messagesLogic.sendPendingMessages(channelId)
    }

    override suspend fun sendAllPendingMessages() {
        return messagesLogic.sendAllPendingMessages()
    }

    override suspend fun sendAllPendingMarkers() {
        return messagesLogic.sendAllPendingMarkers()
    }

    override suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        return messagesLogic.deleteMessage(channelId, message, onlyForMe)
    }

    override suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        return messagesLogic.markMessagesAsRead(channelId, *ids)
    }

    override suspend fun markMessagesAsDelivered(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        return messagesLogic.markMessageAsDelivered(channelId, *ids)
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

    override suspend fun getMessageFromDbById(messageId: Long): SceytMessage? {
        return messagesLogic.getMessageFromDbById(messageId)
    }

    override fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>> = messagesLogic.getOnMessageFlow()

    override suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<User>> {
        return usersLogic.getSceytUsers(ids)
    }

    override suspend fun getUserDbById(id: String): User? {
        return usersLogic.getUserDbById(id)
    }

    override suspend fun getCurrentUser(): User? {
        return usersLogic.getCurrentUser()
    }

    override suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String> {
        return usersLogic.uploadAvatar(avatarUrl)
    }

    override suspend fun updateProfile(firsName: String?, lastName: String?, avatarUrl: String?): SceytResponse<User> {
        return usersLogic.updateProfile(firsName, lastName, avatarUrl)
    }

    override suspend fun updateStatus(status: String): SceytResponse<Boolean> {
        return usersLogic.updateStatus(status)
    }

    override suspend fun getSettings(): SceytResponse<Settings> {
        return usersLogic.getSettings()
    }

    override suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean> {
        return usersLogic.muteNotifications(muteUntil)
    }

    override suspend fun unMuteNotifications(): SceytResponse<Boolean> {
        return usersLogic.unMuteNotifications()
    }
}