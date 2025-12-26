package com.sceyt.chatuikit.persistence

import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chat.models.Types
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.UserListQuery
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.managers.channel.ChannelEventManager
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelOwnerChangedEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.managers.message.event.PollUpdateEvent
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventData
import com.sceyt.chatuikit.data.models.ChangeVoteResponseData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SendMessageResult
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.ChannelInviteKeyData
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytMarker
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.notifications.managers.RealtimeNotificationManager
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInviteKeyInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageMarkerInteractor
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelInviteKeyLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMembersLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistencePollLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceUsersLogic
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class PersistenceMiddleWareImpl(
    private val channelLogic: PersistenceChannelsLogic,
    private val messagesLogic: PersistenceMessagesLogic,
    private val attachmentsLogic: PersistenceAttachmentLogic,
    private val reactionsLogic: PersistenceReactionsLogic,
    private val pollLogic: PersistencePollLogic,
    private val messageMarkerLogic: PersistenceMessageMarkerLogic,
    private val membersLogic: PersistenceMembersLogic,
    private val usersLogic: PersistenceUsersLogic,
    private val connectionLogic: PersistenceConnectionLogic,
    private val channelInviteKeyLogic: PersistenceChannelInviteKeyLogic,
    private val realtimeNotificationManager: RealtimeNotificationManager,
) : ChannelMemberInteractor, MessageInteractor, ChannelInteractor,
    UserInteractor, AttachmentInteractor, MessageMarkerInteractor,
    MessageReactionInteractor, MessagePollInteractor, ChannelInviteKeyInteractor,
    SceytKoinComponent {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Channel events
        ChannelEventManager.onChannelEventFlow.onEach(::onChannelEvent).launchIn(scope)
        ChannelEventManager.onTotalUnreadChangedFlow.onEach(::onChannelUnreadCountUpdatedEvent)
            .launchIn(scope)
        ChannelEventManager.onChannelMembersEventFlow.onEach(::onChannelMemberEvent).launchIn(scope)
        ChannelEventManager.onChannelOwnerChangedEventFlow.onEach(::onChannelOwnerChangedEvent)
            .launchIn(scope)
        // Message events
        ChannelEventManager.onMessageStatusFlow.onEach(::onMessageStatusChangeEvent).launchIn(scope)
        ChannelEventManager.onMarkerReceivedFlow.onEach(::onMessageMarkerEvent).launchIn(scope)
        MessageEventManager.onMessageFlow.onEach(::onMessage).launchIn(scope)
        MessageEventManager.onMessageReactionUpdatedFlow.onEach(::onMessageReactionUpdated)
            .launchIn(scope)
        MessageEventManager.onMessageEditedOrDeletedFlow.onEach(::onMessageEditedOrDeleted)
            .launchIn(scope)
        MessageEventManager.onPollUpdatedFlow.onEach(::onPollUpdated).launchIn(scope)

        // Connection events
        ConnectionEventManager.onChangedConnectStatusFlow.onEach(::onChangedConnectStatus)
            .launchIn(scope)

        // Presence events
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
            .onEach(::onPresenceChanged)
            .launchIn(scope)
    }


    private fun onChannelEvent(event: ChannelActionEvent) {
        scope.launch(Dispatchers.IO) { channelLogic.onChannelEvent(event) }
    }

    private fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData) {
        scope.launch(Dispatchers.IO) { channelLogic.onChannelUnreadCountUpdatedEvent(data) }
    }

    private fun onChannelMemberEvent(data: ChannelMembersEventData) {
        scope.launch(Dispatchers.IO) { membersLogic.onChannelMemberEvent(data) }
    }

    private fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData) {
        scope.launch(Dispatchers.IO) { membersLogic.onChannelOwnerChangedEvent(data) }
    }

    private fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        scope.launch(Dispatchers.IO) { messageMarkerLogic.onMessageStatusChangeEvent(data) }
        scope.launch(Dispatchers.IO) { messagesLogic.onMessageStatusChangeEvent(data) }
        scope.launch(Dispatchers.IO) { channelLogic.onMessageStatusChangeEvent(data) }
    }

    private fun onMessageMarkerEvent(data: MessageMarkerEventData) {
        scope.launch(Dispatchers.IO) { messageMarkerLogic.onMessageMarkerEvent(data) }
        scope.launch(Dispatchers.IO) { messagesLogic.onMessageMarkerEvent(data) }
    }

    private fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        scope.launch(Dispatchers.IO) {
            messagesLogic.onMessage(data)
            channelLogic.onMessage(data)
            realtimeNotificationManager.onMessageReceived(data.first, data.second)
        }
    }

    private fun onMessageReactionUpdated(data: ReactionUpdateEventData) {
        scope.launch(Dispatchers.IO) {
            reactionsLogic.onMessageReactionUpdated(data)
            realtimeNotificationManager.onReactionEvent(data)
        }
    }

    private fun onPollUpdated(event: PollUpdateEvent) {
        scope.launch {
            pollLogic.onPollUpdated(event)
        }
    }

    private fun onMessageEditedOrDeleted(sceytMessage: SceytMessage) {
        scope.launch(Dispatchers.IO) { messagesLogic.onMessageEditedOrDeleted(sceytMessage) }
        scope.launch(Dispatchers.IO) { channelLogic.onMessageEditedOrDeleted(sceytMessage) }
        scope.launch(Dispatchers.IO) {
            realtimeNotificationManager.onMessageStateChanged(message = sceytMessage)
        }
    }

    private fun onChangedConnectStatus(data: ConnectionStateData) {
        scope.launch { connectionLogic.onChangedConnectStatus(data) }
    }

    private fun onPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>) {
        scope.launch(Dispatchers.IO) { usersLogic.onUserPresenceChanged(users) }
        scope.launch(Dispatchers.IO) { channelLogic.onUserPresenceChanged(users) }
    }

    override suspend fun loadChannels(
        offset: Int,
        searchQuery: String,
        loadKey: LoadKeyData?,
        onlyMine: Boolean,
        ignoreDb: Boolean,
        awaitForConnection: Boolean,
        config: ChannelListConfig,
    ) = channelLogic.loadChannels(
        offset = offset,
        searchQuery = searchQuery,
        loadKey = loadKey,
        onlyMine = onlyMine,
        ignoreDb = ignoreDb,
        awaitForConnection = awaitForConnection,
        config = config
    )

    override suspend fun searchChannelsWithUserIds(
        offset: Int,
        searchQuery: String,
        userIds: List<String>,
        config: ChannelListConfig,
        includeSearchByUserDisplayName: Boolean,
        onlyMine: Boolean,
        ignoreDb: Boolean,
        loadKey: LoadKeyData?,
        directChatType: String,
    ) = channelLogic.searchChannelsWithUserIds(
        offset = offset,
        searchQuery = searchQuery,
        userIds = userIds,
        config = config,
        includeSearchByUserDisplayName = includeSearchByUserDisplayName,
        onlyMine = onlyMine,
        ignoreDb = ignoreDb,
        loadKey = loadKey,
        directChatType = directChatType,
    )

    override suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<SceytChannel> {
        return channelLogic.getChannelsBySQLiteQuery(query)
    }

    override suspend fun syncChannels(config: ChannelListConfig): Flow<GetAllChannelsResponse> {
        return channelLogic.syncChannels(config)
    }

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.markChannelAsRead(channelId)
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.markChannelAsUnRead(channelId)
    }

    override suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long> {
        return channelLogic.clearHistory(channelId, forEveryone)
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.blockAndLeaveChannel(channelId)
    }

    override suspend fun unblockChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.unblockChannel(channelId)
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.deleteChannel(channelId)
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.leaveChannel(channelId)
    }

    override suspend fun findOrCreatePendingChannelByMembers(
        data: CreateChannelData,
    ): SceytResponse<SceytChannel> {
        return channelLogic.findOrCreatePendingChannelByMembers(data)
    }

    override suspend fun findOrCreatePendingChannelByUri(
        data: CreateChannelData,
    ): SceytResponse<SceytChannel> {
        return channelLogic.findOrCreatePendingChannelByUri(data)
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        return channelLogic.createChannel(createChannelData)
    }

    override suspend fun muteChannel(
        channelId: Long,
        muteUntil: Long
    ): SceytResponse<SceytChannel> {
        return channelLogic.muteChannel(channelId, muteUntil)
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.unMuteChannel(channelId)
    }

    override suspend fun enableAutoDelete(
        channelId: Long,
        period: Long
    ): SceytResponse<SceytChannel> {
        return channelLogic.enableAutoDelete(channelId, period)
    }

    override suspend fun disableAutoDelete(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.disableAutoDelete(channelId)
    }

    override suspend fun pinChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.pinChannel(channelId)
    }

    override suspend fun unpinChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.unpinChannel(channelId)
    }

    override suspend fun getChannelFromDb(channelId: Long): SceytChannel? {
        return channelLogic.getChannelFromDb(channelId)
    }

    override suspend fun getChannelsFromDb(channelIds: List<Long>): List<SceytChannel> {
        return channelLogic.getChannelsFromDb(channelIds)
    }

    override suspend fun getDirectChannelFromDb(peerId: String): SceytChannel? {
        return channelLogic.getDirectChannelFromDb(peerId)
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.getChannelFromServer(channelId)
    }

    override suspend fun getChannelByInviteKey(inviteKey: String): SceytResponse<SceytChannel> {
        return channelLogic.getChannelByInviteKey(inviteKey)
    }

    override suspend fun getChannelFromServerByUri(uri: String): SceytResponse<SceytChannel?> {
        return channelLogic.getChannelFromServerByUri(uri)
    }

    override suspend fun getChannelsCountFromDb(): Int {
        return channelLogic.getChannelsCountFromDb()
    }

    override suspend fun editChannel(
        channelId: Long,
        data: EditChannelData
    ): SceytResponse<SceytChannel> {
        return channelLogic.editChannel(channelId, data)
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.join(channelId)
    }

    override suspend fun joinWithInviteKey(inviteKey: String): SceytResponse<SceytChannel> {
        return channelLogic.joinWithInviteKey(inviteKey)
    }

    override suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.hideChannel(channelId)
    }

    override suspend fun unHideChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.unHideChannel(channelId)
    }

    override suspend fun updateDraftMessage(draftMessage: DraftMessage) {
        channelLogic.updateDraftMessage(draftMessage)
    }

    override fun getChannelMessageCount(channelId: Long): Flow<Long> {
        return channelLogic.getChannelMessageCount(channelId)
    }

    override fun getTotalUnreadCount(channelTypes: List<String>): Flow<Long> {
        return channelLogic.getTotalUnreadCount(channelTypes)
    }

    override fun loadChannelMembers(
        channelId: Long,
        offset: Int,
        nextToken: String,
        role: String?
    ): Flow<PaginationResponse<SceytMember>> {
        return membersLogic.loadChannelMembers(channelId, offset, nextToken, role)
    }

    override suspend fun loadChannelMembersByIds(
        channelId: Long,
        vararg ids: String
    ): List<SceytMember> {
        return membersLogic.loadChannelMembersByIds(channelId, *ids)
    }

    override suspend fun loadChannelMembersByDisplayName(
        channelId: Long,
        name: String
    ): List<SceytMember> {
        return membersLogic.loadChannelMembersByDisplayName(channelId, name)
    }

    override suspend fun filterOnlyMembersByIds(channelId: Long, ids: List<String>): List<String> {
        return membersLogic.filterOnlyMembersByIds(channelId, ids)
    }

    override suspend fun changeChannelOwner(
        channelId: Long,
        newOwnerId: String
    ): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelOwner(channelId, newOwnerId)
    }

    override suspend fun changeChannelMemberRole(
        channelId: Long,
        vararg member: SceytMember
    ): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelMemberRole(channelId, *member)
    }

    override suspend fun addMembersToChannel(
        channelId: Long,
        members: List<SceytMember>
    ): SceytResponse<SceytChannel> {
        return membersLogic.addMembersToChannel(channelId, members)
    }

    override suspend fun blockAndDeleteMember(
        channelId: Long,
        memberId: String
    ): SceytResponse<SceytChannel> {
        return membersLogic.blockAndDeleteMember(channelId, memberId)
    }

    override suspend fun deleteMember(
        channelId: Long,
        memberId: String
    ): SceytResponse<SceytChannel> {
        return membersLogic.deleteMember(channelId, memberId)
    }

    override suspend fun getMembersCountFromDb(channelId: Long): Int {
        return membersLogic.getMembersCountFromDb(channelId)
    }

    override suspend fun loadPrevMessages(
        conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
        limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadPrevMessages(
            conversationId = conversationId,
            lastMessageId = lastMessageId,
            replyInThread = replyInThread,
            offset = offset,
            limit = limit,
            loadKey = loadKey,
            ignoreDb = ignoreDb
        )
    }

    override suspend fun loadNextMessages(
        conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
        offset: Int, limit: Int, ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNextMessages(
            conversationId = conversationId,
            lastMessageId = lastMessageId,
            replyInThread = replyInThread,
            offset = offset,
            limit = limit,
            ignoreDb = ignoreDb
        )
    }

    override suspend fun loadNearMessages(
        conversationId: Long, messageId: Long, replyInThread: Boolean,
        limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean, ignoreServer: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNearMessages(
            conversationId = conversationId,
            messageId = messageId,
            replyInThread = replyInThread,
            limit = limit,
            loadKey = loadKey,
            ignoreDb = ignoreDb,
            ignoreServer = ignoreServer
        )
    }

    override suspend fun loadNewestMessages(
        conversationId: Long, replyInThread: Boolean, limit: Int,
        loadKey: LoadKeyData, ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNewestMessages(
            conversationId = conversationId,
            replyInThread = replyInThread,
            limit = limit,
            loadKey = loadKey,
            ignoreDb = ignoreDb
        )
    }

    override suspend fun searchMessages(
        conversationId: Long, replyInThread: Boolean,
        query: String,
    ): SceytPagingResponse<List<SceytMessage>> {
        return messagesLogic.searchMessages(conversationId, replyInThread, query)
    }

    override suspend fun getUnreadMentions(
        conversationId: Long,
        direction: Types.Direction,
        messageId: Long,
        limit: Int,
    ): SceytPagingResponse<List<Long>> {
        return messagesLogic.getUnreadMentions(conversationId, direction, messageId, limit)
    }

    override suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>> {
        return messagesLogic.loadNextSearchMessages()
    }

    override suspend fun loadMessagesById(
        conversationId: Long,
        ids: List<Long>
    ): SceytResponse<List<SceytMessage>> {
        return messagesLogic.loadMessagesById(conversationId, ids)
    }

    override suspend fun syncMessagesAfterMessageId(
        conversationId: Long, replyInThread: Boolean,
        messageId: Long,
    ): Flow<SceytResponse<List<SceytMessage>>> {
        return messagesLogic.syncMessagesAfterMessageId(conversationId, replyInThread, messageId)
    }

    override suspend fun syncNearMessages(
        conversationId: Long,
        messageId: Long,
        replyInThread: Boolean
    ): SyncNearMessagesResult {
        return messagesLogic.syncNearMessages(conversationId, messageId, replyInThread)
    }

    override suspend fun sendMessageAsFlow(
        channelId: Long,
        message: Message
    ): Flow<SendMessageResult> {
        return messagesLogic.sendMessageAsFlow(channelId, message)
    }

    override suspend fun sendMessage(channelId: Long, message: Message) {
        return messagesLogic.sendMessage(channelId, message)
    }

    override suspend fun sendMessages(channelId: Long, messages: List<Message>) {
        return messagesLogic.sendMessages(channelId, messages)
    }

    override suspend fun sendSharedFileMessage(channelId: Long, message: Message) {
        return messagesLogic.sendSharedFileMessage(channelId, message)
    }

    override suspend fun sendFrowardMessages(
        channelId: Long,
        vararg messagesToSend: Message
    ): SceytResponse<Boolean> {
        return messagesLogic.sendFrowardMessages(channelId, *messagesToSend)
    }

    override suspend fun sendMessageWithUploadedAttachments(
        channelId: Long,
        message: Message
    ): SceytResponse<SceytMessage> {
        return messagesLogic.sendMessageWithUploadedAttachments(channelId, message)
    }

    override suspend fun sendPendingMessages(channelId: Long) {
        messagesLogic.sendPendingMessages(channelId)
    }

    override suspend fun sendAllPendingMessages() {
        messagesLogic.sendAllPendingMessages()
    }

    override suspend fun sendAllPendingMarkers() {
        messagesLogic.sendAllPendingMarkers()
    }

    override suspend fun sendAllPendingMessageStateUpdates() {
        messagesLogic.sendAllPendingMessageStateUpdates()
    }

    override suspend fun sendAllPendingReactions() {
        reactionsLogic.sendAllPendingReactions()
    }

    override suspend fun markMessagesAs(
        channelId: Long, marker: MarkerType,
        vararg ids: Long,
    ): List<SceytResponse<MessageListMarker>> {
        return messagesLogic.markMessagesAs(channelId, marker, *ids)
    }

    override suspend fun addMessagesMarker(
        channelId: Long,
        marker: String,
        vararg ids: Long
    ): List<SceytResponse<MessageListMarker>> {
        return messagesLogic.addMessagesMarker(channelId, marker, *ids)
    }

    override suspend fun editMessage(
        channelId: Long,
        message: SceytMessage
    ): SceytResponse<SceytMessage> {
        return messagesLogic.editMessage(channelId, message)
    }

    override suspend fun deleteMessage(
        channelId: Long, message: SceytMessage,
        deleteType: DeleteMessageType,
    ): SceytResponse<SceytMessage> {
        return messagesLogic.deleteMessage(channelId, message, deleteType)
    }

    override suspend fun getMessageFromServerById(
        channelId: Long,
        messageId: Long
    ): SceytResponse<SceytMessage> {
        return messagesLogic.getMessageFromServerById(channelId, messageId)
    }

    override suspend fun getMessageFromDbById(messageId: Long): SceytMessage? {
        return messagesLogic.getMessageFromDbById(messageId)
    }

    override suspend fun getMessageFromDbByTid(messageTid: Long): SceytMessage? {
        return messagesLogic.getMessageFromDbByTid(messageTid)
    }

    override suspend fun sendChannelEvent(channelId: Long, event: String) {
        channelLogic.sendChannelEvent(channelId, event)
    }

    override fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>> =
        messagesLogic.getOnMessageFlow()

    override suspend fun getPrevAttachments(
        conversationId: Long,
        lastAttachmentId: Long,
        types: List<String>,
        offset: Int,
        ignoreDb: Boolean,
        loadKeyData: LoadKeyData,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return attachmentsLogic.getPrevAttachments(
            conversationId = conversationId,
            lastAttachmentId = lastAttachmentId,
            types = types,
            offset = offset,
            ignoreDb = ignoreDb,
            loadKeyData = loadKeyData
        )
    }

    override suspend fun getNextAttachments(
        conversationId: Long,
        lastAttachmentId: Long,
        types: List<String>,
        offset: Int,
        ignoreDb: Boolean,
        loadKeyData: LoadKeyData,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return attachmentsLogic.getNextAttachments(
            conversationId = conversationId,
            lastAttachmentId = lastAttachmentId,
            types = types,
            offset = offset,
            ignoreDb = ignoreDb,
            loadKeyData = loadKeyData
        )
    }

    override suspend fun getNearAttachments(
        conversationId: Long,
        attachmentId: Long,
        types: List<String>,
        offset: Int,
        ignoreDb: Boolean,
        loadKeyData: LoadKeyData,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return attachmentsLogic.getNearAttachments(
            conversationId = conversationId,
            attachmentId = attachmentId,
            types = types,
            offset = offset,
            ignoreDb = ignoreDb,
            loadKeyData = loadKeyData
        )
    }

    override suspend fun updateAttachmentIdAndMessageId(message: SceytMessage) {
        attachmentsLogic.updateAttachmentIdAndMessageId(message)
    }

    override suspend fun updateTransferDataByMsgTid(data: TransferData) {
        attachmentsLogic.updateTransferDataByMsgTid(data)
    }

    override suspend fun updateAttachmentWithTransferData(data: TransferData) {
        attachmentsLogic.updateAttachmentWithTransferData(data)
    }

    override suspend fun updateAttachmentFilePathAndMetadata(
        messageTid: Long,
        newPath: String,
        fileSize: Long,
        metadata: String?,
    ) {
        attachmentsLogic.updateAttachmentFilePathAndMetadata(
            messageTid = messageTid,
            newPath = newPath,
            fileSize = fileSize,
            metadata = metadata
        )
    }

    override suspend fun getFileChecksumData(filePath: String?): FileChecksumData? {
        return attachmentsLogic.getFileChecksumData(filePath)
    }

    override suspend fun getLinkPreviewData(link: String?): SceytResponse<LinkPreviewDetails> {
        return attachmentsLogic.getLinkPreviewData(link)
    }

    override suspend fun upsertLinkPreviewData(linkDetails: LinkPreviewDetails) {
        attachmentsLogic.upsertLinkPreviewData(linkDetails)
    }

    override suspend fun loadUsers(query: UserListQuery): SceytResponse<List<SceytUser>> {
        return usersLogic.loadUsers(query)
    }

    override suspend fun loadMoreUsers(): SceytResponse<List<SceytUser>> {
        return usersLogic.loadMoreUsers()
    }

    override suspend fun getUserById(id: String): SceytResponse<SceytUser> {
        return usersLogic.getUserById(id)
    }

    override suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<SceytUser>> {
        return usersLogic.getUsersByIds(ids)
    }

    override suspend fun getUserFromDbById(id: String): SceytUser? {
        return usersLogic.getUserFromDbById(id)
    }

    override suspend fun getUsersFromDbByIds(id: List<String>): List<SceytUser> {
        return usersLogic.getUsersFromDbByIds(id)
    }

    override suspend fun searchLocaleUserByMetadata(
        metadataKeys: List<String>,
        metadataValue: String,
    ): List<SceytUser> {
        return usersLogic.searchLocaleUserByMetadata(metadataKeys, metadataValue)
    }

    override suspend fun getCurrentUser(refreshFromServer: Boolean): SceytUser? {
        return usersLogic.getCurrentUser(refreshFromServer)
    }

    override fun getCurrentUserId(): String? {
        return usersLogic.getCurrentUserId()
    }

    override fun getCurrentUserAsFlow(currentUserId: String?): Flow<SceytUser>? {
        return usersLogic.getCurrentUserAsFlow(currentUserId)
    }

    override suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String> {
        return usersLogic.uploadAvatar(avatarUrl)
    }

    override suspend fun updateProfile(
        username: String,
        firstName: String?,
        lastName: String?,
        avatarUrl: String?,
        metadataMap: Map<String, String>?,
    ): SceytResponse<SceytUser> {
        return usersLogic.updateProfile(username, firstName, lastName, avatarUrl, metadataMap)
    }

    override suspend fun setPresenceState(presenceState: PresenceState): SceytResponse<Boolean> {
        return usersLogic.setPresenceState(presenceState)
    }

    override suspend fun updateStatus(status: String): SceytResponse<Boolean> {
        return usersLogic.updateStatus(status)
    }

    override suspend fun getSettings(): SceytResponse<UserSettings> {
        return usersLogic.getSettings()
    }

    override suspend fun muteNotifications(muteUntil: Long): SceytResponse<Boolean> {
        return usersLogic.muteNotifications(muteUntil)
    }

    override suspend fun unMuteNotifications(): SceytResponse<Boolean> {
        return usersLogic.unMuteNotifications()
    }

    override suspend fun blockUnBlockUser(
        userId: String,
        block: Boolean
    ): SceytResponse<List<SceytUser>> {
        return usersLogic.blockUnBlockUser(userId, block)
    }

    override suspend fun loadReactions(
        messageId: Long,
        offset: Int,
        key: String,
        loadKey: LoadKeyData?,
        ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytReaction>> {
        return reactionsLogic.loadReactions(messageId, offset, key, loadKey, ignoreDb)
    }

    override suspend fun getLocalMessageReactionsById(reactionId: Long): SceytReaction? {
        return reactionsLogic.getLocalMessageReactionsById(reactionId)
    }

    override suspend fun getLocalMessageReactionsByKey(
        messageId: Long,
        key: String
    ): List<SceytReaction> {
        return reactionsLogic.getLocalMessageReactionsByKey(messageId, key)
    }

    override suspend fun addReaction(
        channelId: Long, messageId: Long, key: String, score: Int,
        reason: String, enforceUnique: Boolean,
    ): SceytResponse<SceytMessage> {
        return reactionsLogic.addReaction(channelId, messageId, key, score, reason, enforceUnique)
    }

    override suspend fun deleteReaction(
        channelId: Long,
        messageId: Long,
        scoreKey: String
    ): SceytResponse<SceytMessage> {
        return reactionsLogic.deleteReaction(channelId, messageId, scoreKey)
    }

    override suspend fun toggleVote(
        channelId: Long,
        messageTid: Long,
        pollId: String,
        optionId: String
    ): SceytResponse<ChangeVoteResponseData> {
        return pollLogic.toggleVote(channelId, messageTid, pollId, optionId)
    }

    override suspend fun retractVote(
        channelId: Long,
        messageTid: Long,
        pollId: String
    ): SceytResponse<ChangeVoteResponseData> {
        return pollLogic.retractVote(channelId, messageTid, pollId)
    }

    override suspend fun endPoll(
        channelId: Long,
        messageTid: Long,
        pollId: String
    ): SceytResponse<SceytMessage> {
        return pollLogic.endPoll(channelId, messageTid, pollId)
    }

    override suspend fun sendAllPendingVotes() {
        pollLogic.sendAllPendingVotes()
    }

    override suspend fun getMessageMarkers(
        messageId: Long,
        name: String,
        offset: Int,
        limit: Int
    ): SceytResponse<List<SceytMarker>> {
        return messageMarkerLogic.getMessageMarkers(messageId, name, offset, limit)
    }

    override suspend fun getMessageMarkersDb(
        messageId: Long,
        names: List<String>,
        offset: Int,
        limit: Int
    ): List<SceytMarker> {
        return messageMarkerLogic.getMessageMarkersDb(messageId, names, offset, limit)
    }

    override suspend fun getChannelInviteKeys(channelId: Long): SceytResponse<List<ChannelInviteKeyData>> {
        return channelInviteKeyLogic.getChannelInviteKeys(channelId)
    }

    override suspend fun getChannelInviteKey(
        channelId: Long,
        key: String
    ): SceytResponse<ChannelInviteKeyData> {
        return channelInviteKeyLogic.getChannelInviteKey(channelId, key)
    }

    override suspend fun createChannelInviteKey(
        channelId: Long,
        expireAt: Long,
        maxUses: Int,
        accessPriorHistory: Boolean,
    ): SceytResponse<ChannelInviteKeyData> {
        return channelInviteKeyLogic.createChannelInviteKey(
            channelId = channelId,
            expireAt = expireAt,
            maxUses = maxUses,
            accessPriorHistory = accessPriorHistory
        )
    }

    override suspend fun updateInviteKeySettings(
        channelId: Long,
        key: String,
        expireAt: Long,
        maxUses: Int,
        accessPriorHistory: Boolean,
    ): SceytResponse<Boolean> {
        return channelInviteKeyLogic.updateInviteKeySettings(
            channelId = channelId,
            key = key,
            expireAt = expireAt,
            maxUses = maxUses,
            accessPriorHistory = accessPriorHistory
        )
    }

    override suspend fun regenerateChannelInviteKey(
        channelId: Long,
        key: String,
        deletePermanently: Boolean
    ): SceytResponse<ChannelInviteKeyData> {
        return channelInviteKeyLogic.regenerateChannelInviteKey(channelId, key, deletePermanently)
    }

    override suspend fun revokeChannelInviteKeys(
        channelId: Long,
        keys: List<String>
    ): SceytResponse<Boolean> {
        return channelInviteKeyLogic.revokeChannelInviteKeys(channelId, keys)
    }

    override suspend fun deleteRevokedChannelInviteKeys(
        channelId: Long,
        keys: List<String>
    ): SceytResponse<Boolean> {
        return channelInviteKeyLogic.deleteRevokedChannelInviteKeys(channelId, keys)
    }
}