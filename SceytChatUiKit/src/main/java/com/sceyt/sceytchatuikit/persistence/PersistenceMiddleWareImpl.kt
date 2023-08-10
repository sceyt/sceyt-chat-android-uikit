package com.sceyt.sceytchatuikit.persistence

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.settings.UserSettings
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventsObserver
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelUnreadCountUpdatedEventData
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionStateData
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.CreateChannelData
import com.sceyt.sceytchatuikit.data.models.channels.EditChannelData
import com.sceyt.sceytchatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.data.models.messages.FileChecksumData
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogic
import com.sceyt.sceytchatuikit.persistence.logics.memberslogic.PersistenceMembersLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogic
import com.sceyt.sceytchatuikit.persistence.logics.userslogic.PersistenceUsersLogic
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
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
                                         private val attachmentsLogic: PersistenceAttachmentLogic,
                                         private val reactionsLogic: PersistenceReactionsLogic,
                                         private val membersLogic: PersistenceMembersLogic,
                                         private val usersLogic: PersistenceUsersLogic,
                                         private val connectionLogic: PersistenceConnectionLogic) :
        CoroutineScope, PersistenceMembersMiddleWare, PersistenceMessagesMiddleWare,
        PersistenceChanelMiddleWare, PersistenceUsersMiddleWare, PersistenceAttachmentsMiddleWare,
        PersistenceReactionsMiddleWare, SceytKoinComponent {


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
        launch(Dispatchers.IO) { channelLogic.onChannelEvent(data) }
    }

    private fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData) {
        launch(Dispatchers.IO) { channelLogic.onChannelUnreadCountUpdatedEvent(data) }
    }

    private fun onChannelMemberEvent(data: ChannelMembersEventData) {
        launch(Dispatchers.IO) { membersLogic.onChannelMemberEvent(data) }
    }

    private fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData) {
        launch(Dispatchers.IO) { membersLogic.onChannelOwnerChangedEvent(data) }
    }

    private fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        launch(Dispatchers.IO) {
            messagesLogic.onMessageStatusChangeEvent(data)
            channelLogic.onMessageStatusChangeEvent(data)
        }
    }

    private fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        launch(Dispatchers.IO) {
            messagesLogic.onMessage(data)
            channelLogic.onMessage(data)
        }
    }

    private fun onMessageReactionUpdated(data: ReactionUpdateEventData) {
        launch(Dispatchers.IO) { reactionsLogic.onMessageReactionUpdated(data) }
    }

    private fun onMessageEditedOrDeleted(sceytMessage: SceytMessage) {
        launch(Dispatchers.IO) {
            messagesLogic.onMessageEditedOrDeleted(sceytMessage)
            channelLogic.onMessageEditedOrDeleted(sceytMessage)
        }
    }

    private fun onChangedConnectStatus(data: ConnectionStateData) {
        launch(Dispatchers.IO) { connectionLogic.onChangedConnectStatus(data) }
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String, loadKey: LoadKeyData?,
                                      ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return channelLogic.loadChannels(offset, searchQuery, loadKey, ignoreDb)
    }

    override suspend fun searchChannelsWithUserIds(offset: Int, limit: Int, searchQuery: String, userIds: List<String>,
                                                   loadKey: LoadKeyData?, onlyMine: Boolean, ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return channelLogic.searchChannelsWithUserIds(offset, limit, searchQuery, userIds, loadKey, onlyMine, ignoreDb)
    }

    override suspend fun syncChannels(limit: Int): Flow<GetAllChannelsResponse> {
        return channelLogic.syncChannels(limit)
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

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.deleteChannel(channelId)
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        return channelLogic.leaveChannel(channelId)
    }

    override suspend fun findOrCreateDirectChannel(user: User): SceytResponse<SceytChannel> {
        return channelLogic.findOrCreateDirectChannel(user)
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

    override suspend fun getDirectChannelFromDb(peerId: String): SceytChannel? {
        return channelLogic.getDirectChannelFromDb(peerId)
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.getChannelFromServer(channelId)
    }

    override suspend fun getChannelFromServerByUrl(uri: String): SceytResponse<List<SceytChannel>> {
        return channelLogic.getChannelFromServerByUrl(uri)
    }

    override suspend fun getChannelsCountFromDb(): Int {
        return channelLogic.getChannelsCountFromDb()
    }

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        return channelLogic.editChannel(channelId, data)
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.join(channelId)
    }

    override suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelLogic.hideChannel(channelId)
    }

    override suspend fun updateDraftMessage(channelId: Long, message: String?, mentionUsers: List<Mention>) {
        channelLogic.updateDraftMessage(channelId, message, mentionUsers)
    }

    override fun getTotalUnreadCount(): Flow<Int> {
        return channelLogic.getTotalUnreadCount()
    }

    override suspend fun loadChannelMembers(channelId: Long, offset: Int, role: String?): Flow<PaginationResponse<SceytMember>> {
        return membersLogic.loadChannelMembers(channelId, offset, role)
    }

    override suspend fun loadChannelMembersByIds(channelId: Long, vararg ids: String): List<SceytMember> {
        return membersLogic.loadChannelMembersByIds(channelId, *ids)
    }

    override suspend fun loadChannelMembersByDisplayName(channelId: Long, name: String): List<SceytMember> {
        return membersLogic.loadChannelMembersByDisplayName(channelId, name)
    }

    override suspend fun filterOnlyMembersByIds(channelId: Long, ids: List<String>): List<String> {
        return membersLogic.filterOnlyMembersByIds(channelId, ids)
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>> {
        return membersLogic.blockUnBlockUser(userId, block)
    }

    override suspend fun changeChannelOwner(channelId: Long, newOwnerId: String): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelOwner(channelId, newOwnerId)
    }

    override suspend fun changeChannelMemberRole(channelId: Long, vararg member: SceytMember): SceytResponse<SceytChannel> {
        return membersLogic.changeChannelMemberRole(channelId, *member)
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

    override suspend fun getMembersCountDb(channelId: Long): Int {
        return membersLogic.getMembersCountDb(channelId)
    }

    override suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean, offset: Int,
                                          limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadPrevMessages(conversationId, lastMessageId, replyInThread, offset, limit, loadKey, ignoreDb)
    }

    override suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                          offset: Int, limit: Int, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNextMessages(conversationId, lastMessageId, replyInThread, offset, limit, ignoreDb)
    }

    override suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                          limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean, ignoreServer: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNearMessages(conversationId, messageId, replyInThread, limit, loadKey, ignoreDb, ignoreServer)
    }

    override suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, limit: Int,
                                            loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return messagesLogic.loadNewestMessages(conversationId, replyInThread, limit, loadKey, ignoreDb)
    }

    override suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>> {
        return messagesLogic.loadMessagesById(conversationId, ids)
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

    override suspend fun sendSharedFileMessage(channelId: Long, message: Message) {
        return messagesLogic.sendSharedFileMessage(channelId, message)
    }

    override suspend fun sendFrowardMessages(channelId: Long, messagesToSend: List<Message>): SceytResponse<Boolean> {
        return messagesLogic.sendFrowardMessages(channelId, messagesToSend)
    }

    override suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message): SceytResponse<SceytMessage> {
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

    override suspend fun sendAllPendingReactions() {
        reactionsLogic.sendAllPendingReactions()
    }

    override suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        return messagesLogic.deleteMessage(channelId, message, onlyForMe)
    }

    override suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>> {
        return messagesLogic.markMessagesAsRead(channelId, *ids)
    }

    override suspend fun markMessagesAsDelivered(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>> {
        return messagesLogic.markMessageAsDelivered(channelId, *ids)
    }

    override suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        return messagesLogic.editMessage(channelId, message)
    }

    override suspend fun getMessageFromServerById(channelId: Long, messageId: Long): SceytResponse<SceytMessage> {
        return messagesLogic.getMessageFromServerById(channelId, messageId)
    }

    override suspend fun getMessageDbById(messageId: Long): SceytMessage? {
        return messagesLogic.getMessageDbById(messageId)
    }

    override fun getOnMessageFlow(): SharedFlow<Pair<SceytChannel, SceytMessage>> = messagesLogic.getOnMessageFlow()

    override suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadEntity> {
        return attachmentsLogic.getAllPayLoadsByMsgTid(tid)
    }

    override suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>, offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData): Flow<PaginationResponse<AttachmentWithUserData>> {
        return attachmentsLogic.getPrevAttachments(conversationId, lastAttachmentId, types, offset, ignoreDb, loadKeyData)
    }

    override suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>, offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData): Flow<PaginationResponse<AttachmentWithUserData>> {
        return attachmentsLogic.getNextAttachments(conversationId, lastAttachmentId, types, offset, ignoreDb, loadKeyData)
    }

    override suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>, offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData): Flow<PaginationResponse<AttachmentWithUserData>> {
        return attachmentsLogic.getNearAttachments(conversationId, attachmentId, types, offset, ignoreDb, loadKeyData)
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

    override suspend fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?) {
        attachmentsLogic.updateAttachmentFilePathAndMetadata(messageTid, newPath, fileSize, metadata)
    }

    override suspend fun getFileChecksumData(filePath: String?): FileChecksumData? {
        return attachmentsLogic.getFileChecksumData(filePath)
    }

    override suspend fun loadUsers(query: String): SceytResponse<List<User>> {
        return usersLogic.loadUsers(query)
    }

    override suspend fun loadMoreUsers(): SceytResponse<List<User>> {
        return usersLogic.loadMoreUsers()
    }

    override suspend fun getUsersByIds(ids: List<String>): SceytResponse<List<User>> {
        return usersLogic.getSceytUsers(ids)
    }

    override suspend fun getUserDbById(id: String): User? {
        return usersLogic.getUserDbById(id)
    }

    override suspend fun getCurrentUser(): User? {
        return usersLogic.getCurrentUser()
    }

    override fun getCurrentUserAsFlow(): Flow<User> {
        return usersLogic.getCurrentUserAsFlow()
    }

    override suspend fun uploadAvatar(avatarUrl: String): SceytResponse<String> {
        return usersLogic.uploadAvatar(avatarUrl)
    }

    override suspend fun updateProfile(firsName: String?, lastName: String?, avatarUrl: String?): SceytResponse<User> {
        return usersLogic.updateProfile(firsName, lastName, avatarUrl)
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

    override suspend fun loadReactions(messageId: Long, offset: Int, key: String, loadKey: LoadKeyData?, ignoreDb: Boolean): Flow<PaginationResponse<SceytReaction>> {
        return reactionsLogic.loadReactions(messageId, offset, key, loadKey, ignoreDb)
    }

    override suspend fun getMessageReactionsDbByKey(messageId: Long, key: String): List<SceytReaction> {
        return reactionsLogic.getMessageReactionsDbByKey(messageId, key)
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int): SceytResponse<SceytMessage> {
        return reactionsLogic.addReaction(channelId, messageId, key, score)
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String, isPending: Boolean): SceytResponse<SceytMessage> {
        return reactionsLogic.deleteReaction(channelId, messageId, scoreKey, isPending)
    }
}