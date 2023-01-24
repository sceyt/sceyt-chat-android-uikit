package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import android.app.Application
import android.util.Log
import androidx.work.WorkManager
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.*
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum.Displayed
import com.sceyt.sceytchatuikit.data.models.messages.SelfMarkerTypeEnum.Received
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingMarkersDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.PendingMarkersEntity
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.mappers.*
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.LoadKeyType
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class PersistenceMessagesLogicImpl(
        private val application: Application,
        private val messageDao: MessageDao,
        private val pendingMarkersDao: PendingMarkersDao,
        private val reactionDao: ReactionDao,
        private val persistenceChannelsLogic: PersistenceChannelsLogic,
        private val userDao: UserDao,
        private val messagesRepository: MessagesRepository,
        private val preference: SceytSharedPreference,
        private val messagesCache: MessagesCache
) : PersistenceMessagesLogic, SceytKoinComponent, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    private val onMessageFlow: MutableSharedFlow<Pair<SceytChannel, SceytMessage>> = MutableSharedFlow(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>, sendDeliveryMarker: Boolean) {
        val message = data.second

        message.parent?.let { parent ->
            saveMessagesToDb(arrayListOf(message, parent))
        } ?: run { saveMessagesToDb(arrayListOf(message)) }

        onMessageFlow.tryEmit(data)

        if (message.incoming && sendDeliveryMarker)
            markMessagesAs(data.first.id, Received, message.id)
    }

    override fun onFcmMessage(data: Pair<SceytChannel, SceytMessage>) {
        launch {
            onMessage(data, false)
            persistenceChannelsLogic.onFcmMessage(data)
        }
    }

    override suspend fun onChannelEvent(data: ChannelEventData) {
        when (data.eventType) {
            Deleted, ClearedHistory, Left, Hidden -> {
                data.channelId?.let { messageDao.deleteAllMessages(it) }
            }
            else -> return
        }
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        val updatedMessages = messageDao.updateMessageStatusWithBefore(data.status, data.messageIds.maxOf { it })
        messagesCache.updateMessagesStatus(data.status, *updatedMessages.map { it.tid }.toLongArray())
    }

    override suspend fun onMessageReactionUpdated(data: Message) {
        reactionDao.insertReactionsAndScores(
            messageId = data.id,
            reactionsDb = data.lastReactions.map { it.toReactionEntity(data.id) },
            scoresDb = data.reactionScores.map { it.toReactionScoreEntity(data.id) })
        messagesCache.messageUpdated(data.toSceytUiMessage())
    }

    override suspend fun onMessageEditedOrDeleted(data: SceytMessage) {
        messageDao.updateMessage(data.toMessageEntity())
        messagesCache.messageUpdated(data)
        if (data.state == MessageState.Deleted)
            deletedPayloads(data.id, data.tid)
    }

    override suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                          offset: Int, loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadPrev, conversationId, lastMessageId, replyInThread, offset, loadKey, ignoreDb)
    }

    override suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                          offset: Int, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNext, conversationId, lastMessageId, replyInThread, offset, ignoreDb = ignoreDb)
    }

    override suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                          loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNear, conversationId, messageId, replyInThread, 0, loadKey, ignoreDb)
    }

    override suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, loadKey: LoadKeyData,
                                            ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNewest, conversationId, 0, replyInThread, 0, loadKey, ignoreDb)
    }

    override suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>> {
        val response = messagesRepository.loadMessagesById(conversationId, ids)
        if (response is SceytResponse.Success)
            saveMessagesToDb(response.data)
        return response
    }

    override suspend fun syncMessagesAfterMessageId(conversationId: Long, replyInThread: Boolean,
                                                    messageId: Long): Flow<SceytResponse<List<SceytMessage>>> = callbackFlow {
        ConnectionEventsObserver.awaitToConnectSceyt()
        messagesRepository.loadAllMessagesAfter(conversationId, replyInThread, messageId)
            .onCompletion { channel.close() }
            .collect {
                if (it is SceytResponse.Success) {
                    it.data?.let { messages ->
                        saveMessagesToDb(messages)
                        messagesCache.upsertMessages(*messages.toTypedArray())
                        markChannelMessagesAsDelivered(conversationId, messages)
                    }
                }
                trySend(it)
            }
        awaitClose()
    }

    override suspend fun getMessagesByType(channelId: Long, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>> {
        val response = messagesRepository.getMessagesByType(channelId, lastMessageId, type)
        if (response is SceytResponse.Success) {
            val tIds = getMessagesTid(response.data)
            val payloads = messageDao.getAllAttachmentPayLoadsByMsgTid(*tIds.toLongArray())
            response.data?.forEach {
                findAndUpdateAttachmentPayLoads(it, payloads)
                it.parent?.let { parent -> findAndUpdateAttachmentPayLoads(parent, payloads) }
            }
        }
        return response
    }

    override suspend fun sendMessage(channelId: Long, message: Message) {
        val tmpMessage = tmpMessageToSceytMessage(channelId, message)
        MessageEventsObserver.emitOutgoingMessage(tmpMessage)
        insertTmpMessageToDb(tmpMessage)
        messagesCache.add(tmpMessage)

        if (message.attachments.isNullOrEmpty().not()) {
            SendAttachmentWorkManager.schedule(application, tmpMessage.tid)
        } else {
            val response = messagesRepository.sendMessage(channelId, message)
            onMessageSentResponse(channelId, response)
        }
    }

    override suspend fun sendMessages(channelId: Long, messages: List<Message>) {
        messages.forEach {
            sendMessage(channelId, it)
        }
    }

    override suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult> = callbackFlow {
        val tmpMessage = tmpMessageToSceytMessage(channelId, message)
        trySend(SendMessageResult.TempMessage(tmpMessage))
        insertTmpMessageToDb(tmpMessage)
        messagesCache.add(tmpMessage)

        MessageEventsObserver.emitOutgoingMessage(tmpMessage)

        if (message.attachments.isNullOrEmpty().not()) {
            SendAttachmentWorkManager.schedule(application, tmpMessage.tid)
        } else {
            messagesRepository.sendMessageAsFlow(channelId, message)
                .onCompletion { channel.close() }
                .collect { result ->
                    if (result is SendMessageResult.Response) {
                        onMessageSentResponse(channelId, result.response)
                        trySend(result)
                    }
                }
        }
        awaitClose()
    }

    override suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message) {
        val response = messagesRepository.sendMessage(channelId, message)
        onMessageSentResponse(channelId, response)
    }

    private fun tmpMessageToSceytMessage(channelId: Long, message: Message): SceytMessage {
        val tmpMessage = message.toSceytUiMessage().apply {
            createdAt = System.currentTimeMillis()
            from = ClientWrapper.currentUser ?: User(preference.getUserId())
            this.channelId = channelId
            attachments?.map {
                it.transferState = TransferState.Uploading
                it.progressPercent = 0f
                if (!it.existThumb())
                    it.addAttachmentMetadata(application)
            }
        }
        return tmpMessage
    }

    private suspend fun insertTmpMessageToDb(message: SceytMessage) {
        val tmpMessageDb = message.toMessageDb().also {
            it.messageEntity.id = null
            if (message.replyInThread)
                it.messageEntity.channelId = message.parent?.id ?: 0
        }
        messageDao.insertMessage(tmpMessageDb)
        persistenceChannelsLogic.updateLastMessageWithLastRead(message.channelId, message)
    }

    private suspend fun onMessageSentResponse(channelId: Long, response: SceytResponse<SceytMessage>) {
        if (response is SceytResponse.Success) {
            response.data?.let { responseMsg ->
                messageDao.updateMessageByParams(
                    tid = responseMsg.tid, serverId = responseMsg.id,
                    date = responseMsg.createdAt, status = DeliveryStatus.Sent)

                messagesCache.messageUpdated(responseMsg)
                persistenceChannelsLogic.updateLastMessageWithLastRead(channelId, responseMsg)
            }
        }
    }

    override suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            messageDao.deleteMessageByTid(message.tid)
            messagesCache.deleteMessage(message.tid)
            persistenceChannelsLogic.onMessageEditedOrDeleted(message)
            WorkManager.getInstance(application).cancelAllWorkByTag(message.tid.toString())
            return SceytResponse.Success(message.apply { state = MessageState.Deleted })
        }
        val response = messagesRepository.deleteMessage(channelId, message.id, onlyForMe)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                onMessageEditedOrDeleted(resultMessage)
                persistenceChannelsLogic.onMessageEditedOrDeleted(resultMessage)
            }
        }
        return response
    }

    override suspend fun sendPendingMessages(channelId: Long) {
        val pendingMessages = messageDao.getPendingMessages(channelId)
        if (pendingMessages.isNotEmpty()) {
            pendingMessages.forEach {
                val message = it.toMessage()
                if (message.attachments.isNullOrEmpty().not()) {
                    SendAttachmentWorkManager.schedule(application, message.tid)
                } else {
                    val response = messagesRepository.sendMessage(channelId, message)
                    onMessageSentResponse(channelId, response)
                }
            }
        }
    }

    override suspend fun sendAllPendingMessages() {
        val pendingMessages = messageDao.getAllPendingMessages()
        if (pendingMessages.isNotEmpty()) {
            pendingMessages.forEach {
                val message = it.toMessage()
                if (message.attachments.isNullOrEmpty().not()) {
                    SendAttachmentWorkManager.schedule(application, message.tid)
                } else {
                    val response = messagesRepository.sendMessage(it.messageEntity.channelId, message)
                    if (response is SceytResponse.Success) {
                        response.data?.let { responseMsg ->
                            messageDao.updateMessageByParams(
                                tid = responseMsg.tid, serverId = responseMsg.id,
                                date = responseMsg.createdAt, status = DeliveryStatus.Sent)

                            messagesCache.messageUpdated(responseMsg)
                        }
                    } else Log.e("sendMessage", "send pending message error-> ${response.message}")
                }
            }
        }
    }

    override suspend fun sendAllPendingMarkers() {
        val pendingMarkers = pendingMarkersDao.getAllMarkers()
        if (pendingMarkers.isNotEmpty()) {
            val groupByChannel = pendingMarkers.groupBy { it.channelId }
            for ((channelId, messages) in groupByChannel) {
                val messagesByStatus = messages.groupBy { it.status }
                for ((status, msg) in messagesByStatus)
                    markMessagesAs(channelId, status, *msg.map { it.messageId }.toLongArray())
            }
        }
    }

    override suspend fun markMessageAsDelivered(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        return markMessagesAs(channelId, Received, *ids)
    }

    override suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): SceytResponse<MessageListMarker> {
        return markMessagesAs(channelId, Displayed, *ids)
    }

    override suspend fun editMessage(id: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        val response = messagesRepository.editMessage(id, message)
        if (response is SceytResponse.Success) {
            response.data?.let { updatedMsg ->
                messageDao.updateMessage(updatedMsg.toMessageEntity())
                messagesCache.messageUpdated(updatedMsg)
                persistenceChannelsLogic.onMessageEditedOrDeleted(updatedMsg)
            }
        }
        return response
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        val response = messagesRepository.addReaction(channelId, messageId, scoreKey)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                message.lastReactions?.let {
                    messageDao.insertReactions(it.map { reaction -> reaction.toReactionEntity(messageId) })
                }
                message.reactionScores?.let {
                    messageDao.insertReactionScores(it.map { score -> score.toReactionScoreEntity(messageId) })
                }
                messagesCache.messageUpdated(message)
            }
        }
        return response
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        val response = messagesRepository.deleteReaction(channelId, messageId, scoreKey)
        if (response is SceytResponse.Success) {
            response.data?.let { message ->
                message.reactionScores?.let {
                    val fromId = preference.getUserId()
                    if (fromId != null)
                        reactionDao.deleteReactionAndScore(messageId, scoreKey, fromId)
                }
                messagesCache.messageUpdated(message)
            }
        }
        return response
    }

    override suspend fun getMessageFromDbById(messageId: Long): SceytMessage? {
        return messageDao.getMessageById(messageId)?.toSceytMessage()
    }

    override suspend fun getMessageFromDbByTid(tid: Long): SceytMessage? {
        return messageDao.getMessageByTid(tid)?.toSceytMessage()
    }

    override fun getOnMessageFlow() = onMessageFlow.asSharedFlow()


    private fun loadMessages(loadType: LoadType, conversationId: Long, messageId: Long,
                             replyInThread: Boolean, offset: Int, loadKey: LoadKeyData = LoadKeyData(value = messageId),
                             ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            if (offset == 0) messagesCache.clear()

            // Load from database
            if (!ignoreDb)
                trySend(getMessagesDbByLoadType(loadType, conversationId, messageId, offset, loadKey))
            // Load from server
            trySend(getMessagesServerByLoadType(loadType, conversationId, messageId, offset, replyInThread,
                loadKey, ignoreDb))

            channel.close()
            awaitClose()
        }
    }

    private suspend fun getMessagesDbByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                offset: Int, loadKey: LoadKeyData): PaginationResponse.DBResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        val messages: List<SceytMessage>

        when (loadType) {
            LoadPrev -> {
                messages = getPrevMessagesDb(channelId, lastMessageId, offset)
                hasPrev = messages.size == MESSAGES_LOAD_SIZE
            }
            LoadNext -> {
                messages = getNextMessagesDb(channelId, lastMessageId, offset)
                hasNext = messages.size == MESSAGES_LOAD_SIZE
            }
            LoadNear -> {
                val data = getNearMessagesDb(channelId, lastMessageId, offset)
                messages = data.data.map { it.toSceytMessage() }
                hasPrev = data.hasPrev
                hasNext = data.hasNext
            }
            LoadNewest -> {
                messages = getPrevMessagesDb(channelId, Long.MAX_VALUE, offset)
                hasPrev = messages.size == MESSAGES_LOAD_SIZE
            }
        }

        messagesCache.addAll(messages, false)

        // Mark messages as received
        markChannelMessagesAsDelivered(channelId, messages)

        return PaginationResponse.DBResponse(messages, loadKey, offset, hasNext, hasPrev, loadType)
    }

    private suspend fun getMessagesServerByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                    offset: Int, replyInThread: Boolean, loadKey: LoadKeyData = LoadKeyData(value = lastMessageId),
                                                    ignoreDb: Boolean): PaginationResponse.ServerResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        var hasDiff: Boolean
        var forceHasDiff = false
        var messages: List<SceytMessage> = emptyList()
        val response: SceytResponse<List<SceytMessage>>

        when (loadType) {
            LoadPrev -> {
                response = messagesRepository.getPrevMessages(channelId, lastMessageId, replyInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
                    // Check maybe messages was cleared
                    if (offset == 0 && messages.isEmpty()) {
                        messageDao.deleteAllMessages(channelId)
                        messagesCache.clear()
                        forceHasDiff = true
                    }
                }
            }
            LoadNext -> {
                response = messagesRepository.getNextMessages(channelId, lastMessageId, replyInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasNext = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }
            LoadNear -> {
                response = messagesRepository.getNearMessages(channelId, lastMessageId, replyInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    val groupOldAndNewData = messages.groupBy { it.id > lastMessageId }

                    val newest = groupOldAndNewData[true]
                    val oldest = groupOldAndNewData[false]

                    hasNext = (newest?.size ?: 0) >= MESSAGES_LOAD_SIZE / 2
                    hasPrev = (oldest?.size ?: 0) >= MESSAGES_LOAD_SIZE / 2
                }
            }
            LoadNewest -> {
                response = messagesRepository.getPrevMessages(channelId, Long.MAX_VALUE, replyInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }
        }

        saveMessagesToDb(messages)
        val tIds = getMessagesTid(messages)
        val payloads = messageDao.getAllAttachmentPayLoadsByMsgTid(*tIds.toLongArray())

        messages.forEach {
            findAndUpdateAttachmentPayLoads(it, payloads)
            it.parent?.let { parent -> findAndUpdateAttachmentPayLoads(parent, payloads) }
        }

        if (loadType == LoadNear && loadKey.key == LoadKeyType.ScrollToMessageById.longValue)
            messagesCache.clear()

        hasDiff = messagesCache.addAll(messages, true)

        if (forceHasDiff) hasDiff = true

        // Mark messages as received
        markChannelMessagesAsDelivered(channelId, messages)

        return PaginationResponse.ServerResponse(
            data = response, cacheData = messagesCache.getSorted(),
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb)
    }

    private fun findAndUpdateAttachmentPayLoads(message: SceytMessage, payloads: List<AttachmentPayLoadEntity>) {
        payloads.find { payLoad -> payLoad.messageTid == message.tid }?.let { entity ->
            message.attachments?.forEach { attachment ->
                attachment.transferState = entity.transferState
                attachment.progressPercent = entity.progressPercent
                attachment.filePath = entity.filePath
                attachment.url = entity.url
            }
        }
    }

    private suspend fun getPrevMessagesDb(channelId: Long, lastMessageId: Long, offset: Int): List<SceytMessage> {
        var lastMsgId = lastMessageId
        if (lastMessageId == 0L)
            lastMsgId = Long.MAX_VALUE

        var messages = messageDao.getOldestThenMessages(channelId, lastMsgId, MESSAGES_LOAD_SIZE).reversed()

        if (offset == 0)
            messages = getPendingMessagesAndAddToList(channelId, messages.toArrayList())

        return messages.map { messageDb -> messageDb.toSceytMessage() }
    }

    private suspend fun getNextMessagesDb(channelId: Long, lastMessageId: Long, offset: Int): List<SceytMessage> {
        var messages = messageDao.getNewestThenMessage(channelId, lastMessageId, MESSAGES_LOAD_SIZE)

        if (offset == 0)
            messages = getPendingMessagesAndAddToList(channelId, messages.toArrayList())

        return messages.map { messageDb -> messageDb.toSceytMessage() }
    }

    private suspend fun getNearMessagesDb(channelId: Long, messageId: Long, offset: Int): LoadNearData<MessageDb> {
        val data = messageDao.getNearMessages(channelId, messageId, MESSAGES_LOAD_SIZE)
        val messages = data.data

        if (offset == 0)
            data.data = getPendingMessagesAndAddToList(channelId, messages.toArrayList())

        return data
    }

    private suspend fun getPendingMessagesAndAddToList(channelId: Long, list: ArrayList<MessageDb>): ArrayList<MessageDb> {
        val pendingMessage = messageDao.getPendingMessages(channelId)

        if (pendingMessage.isNotEmpty())
            list.addAll(pendingMessage)
        return list
    }

    private suspend fun saveMessagesToDb(list: List<SceytMessage>?) {
        if (list.isNullOrEmpty()) return

        val usersDb = arrayListOf<UserEntity>()

        val messagesDb = arrayListOf<MessageDb>()
        for (message in list) {
            messagesDb.add(message.toMessageDb())
            message.parent?.let { parent ->
                if (parent.id != 0L) {
                    messagesDb.add(parent.toMessageDb())
                    if (parent.incoming)
                        parent.from?.let { user -> usersDb.add(user.toUserEntity()) }
                }
            }
        }
        messageDao.insertMessages(messagesDb)

        // Update users
        list.filter { it.incoming && it.from != null }.map { it.from!! }.toSet().let { users ->
            if (users.isNotEmpty())
                usersDb.addAll(users.map { it.toUserEntity() })
        }

        // Users which added reaction
        val usersByReaction = list.flatMap {
            it.lastReactions?.toMutableList() ?: mutableListOf()
        }.map {
            it.user.toUserEntity()
        }
        usersDb.addAll(usersByReaction)

        userDao.insertUsers(usersDb)
    }

    private suspend fun deletedPayloads(id: Long, tid: Long) {
        messageDao.deleteAttachmentsChunked(listOf(tid))
        messageDao.deleteAttachmentsPayloadsChunked(listOf(tid))
        reactionDao.deleteAllReactionsAndScores(id)
    }

    private fun getMessagesTid(messages: List<SceytMessage>?): List<Long> {
        val tIds = mutableListOf<Long>()
        messages?.forEach {
            tIds.add(it.tid)
            it.parent?.let { parent -> tIds.add(parent.tid) }
        }
        return tIds
    }

    private suspend fun markChannelMessagesAsDelivered(channelId: Long, messages: List<SceytMessage>) {
        val notDisplayedMessages = messages.filter {
            it.incoming && it.selfMarkers?.contains(Received.value()) != true
        }
        if (notDisplayedMessages.isNotEmpty())
            markMessageAsDelivered(channelId, *notDisplayedMessages.map { it.id }.toLongArray())
    }

    private suspend fun markMessagesAs(channelId: Long, status: SelfMarkerTypeEnum, vararg ids: Long): SceytResponse<MessageListMarker> {
        addPendingMarkerToDb(channelId, status, *ids)

        val response = if (status == Displayed)
            messagesRepository.markAsRead(channelId, *ids)
        else messagesRepository.markAsDelivered(channelId, *ids)
        onMarkerResponse(channelId, response, status, *ids)
        return response
    }

    private suspend fun addPendingMarkerToDb(channelId: Long, status: SelfMarkerTypeEnum, vararg ids: Long) {
        try {
            val list = ids.map { PendingMarkersEntity(channelId = channelId, messageId = it, status = status) }
            pendingMarkersDao.insertMany(list)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't insert pending markers.")
        }
    }

    private suspend fun onMarkerResponse(channelId: Long, response: SceytResponse<MessageListMarker>, status: SelfMarkerTypeEnum, vararg ids: Long) {
        if (response is SceytResponse.Success) {
            response.data?.let { messageListMarker ->
                val deliveryStatus = status.toDeliveryStatus()
                messageDao.updateMessagesStatus(channelId, messageListMarker.messageIds, deliveryStatus)
                val tIds = messageDao.getMessageTIdsByIds(*ids)
                messagesCache.updateMessagesStatus(deliveryStatus, *tIds.toLongArray())

                pendingMarkersDao.deleteMessagesMarkersByStatus(response.data.messageIds, status)
                ids.forEach {
                    messageDao.updateMessageSelfMarkers(channelId, it, status.value())
                }
            }
        }
    }
}