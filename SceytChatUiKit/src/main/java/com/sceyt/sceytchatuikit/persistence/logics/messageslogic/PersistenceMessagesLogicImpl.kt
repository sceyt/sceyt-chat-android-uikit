package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import android.content.Context
import androidx.work.await
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.SDKErrorTypeEnum
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageEventsObserver
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventData
import com.sceyt.sceytchatuikit.data.messageeventobserver.ReactionUpdateEventEnum
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.LoadNearData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.SendMessageResult
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MarkerTypeEnum
import com.sceyt.sceytchatuikit.data.models.messages.MarkerTypeEnum.Displayed
import com.sceyt.sceytchatuikit.data.models.messages.MarkerTypeEnum.Received
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.dao.AttachmentDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingMarkersDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingMessageStateDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.entity.pendings.PendingMarkerEntity
import com.sceyt.sceytchatuikit.persistence.entity.pendings.PendingMessageStateEntity
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogic
import com.sceyt.sceytchatuikit.persistence.mappers.addAttachmentMetadata
import com.sceyt.sceytchatuikit.persistence.mappers.existThumb
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytReaction
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.sceytchatuikit.persistence.workers.SendForwardMessagesWorkManager
import com.sceyt.sceytchatuikit.pushes.RemoteMessageData
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MESSAGES_LOAD_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

internal class PersistenceMessagesLogicImpl(
        private val context: Context,
        private val messageDao: MessageDao,
        private val attachmentDao: AttachmentDao,
        private val pendingMarkersDao: PendingMarkersDao,
        private val reactionDao: ReactionDao,
        private val userDao: UserDao,
        private val pendingMessageStateDao: PendingMessageStateDao,
        private val fileTransferService: FileTransferService,
        private val messagesRepository: MessagesRepository,
        private val preference: SceytSharedPreference,
        private val messagesCache: MessagesCache,
        private val channelCache: ChannelsCache
) : PersistenceMessagesLogic, SceytKoinComponent {

    private val persistenceChannelsLogic: PersistenceChannelsLogic by inject()
    private val persistenceAttachmentLogic: PersistenceAttachmentLogic by inject()
    private val persistenceReactionLogic: PersistenceReactionsLogic by inject()
    private val createChannelAndSendMessageMutex = Mutex()

    private val onMessageFlow: MutableSharedFlow<Pair<SceytChannel, SceytMessage>> = MutableSharedFlow(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>, sendDeliveryMarker: Boolean) {
        val message = data.second

        saveMessagesToDb(arrayListOf(message))

        messagesCache.add(data.first.id, message)
        onMessageFlow.tryEmit(data)

        if (message.incoming && sendDeliveryMarker)
            markMessagesAs(data.first.id, Received, message.id)
    }

    override suspend fun onFcmMessage(data: RemoteMessageData) {
        val message = data.message
        if (message?.id == 0L) return
        val channelDb = persistenceChannelsLogic.getChannelFromDb(data.channel?.id
                ?: return)
        if (channelDb != null && (message?.createdAt ?: 0) <= channelDb.messagesClearedAt)
            return

        val messageDb = messageDao.getMessageById(message?.id ?: return)

        val isReaction = data.reaction != null

        if (messageDb == null && !isReaction) {
            saveMessagesToDb(arrayListOf(message), false)
            messagesCache.add(data.channel.id, message)
            onMessageFlow.tryEmit(Pair(data.channel, message))
            persistenceChannelsLogic.onFcmMessage(data)
        }

        if (messageDb != null && isReaction)
            persistenceReactionLogic.onMessageReactionUpdated(ReactionUpdateEventData(
                messageDb.toSceytMessage(), data.reaction!!, ReactionUpdateEventEnum.Add))
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        val updatedMessages = messageDao.updateMessageStatusWithBefore(data.channel.id, data.status, data.messageIds.maxOf { it })
        messagesCache.updateMessagesStatus(data.channel.id, data.status, *updatedMessages.map { it.tid }.toLongArray())
    }

    override suspend fun onMessageEditedOrDeleted(data: SceytMessage) {
        val selfReactions = reactionDao.getSelfReactionsByMessageId(data.id, SceytKitClient.myId.toString())
        data.userReactions = selfReactions.map { it.toSceytReaction() }.toTypedArray()
        messageDao.updateMessage(data.toMessageEntity(false))
        messagesCache.messageUpdated(data.channelId, data)
        if (data.state == MessageState.Deleted)
            deletedPayloads(data.id, data.tid)
    }

    override suspend fun loadPrevMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                          offset: Int, limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadPrev, conversationId, lastMessageId, replyInThread, offset, limit, loadKey, ignoreDb)
    }

    override suspend fun loadNextMessages(conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
                                          offset: Int, limit: Int, ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNext, conversationId, lastMessageId, replyInThread, offset, limit, ignoreDb = ignoreDb)
    }

    override suspend fun loadNearMessages(conversationId: Long, messageId: Long, replyInThread: Boolean,
                                          limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean, ignoreServer: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNear, conversationId, messageId, replyInThread, 0, limit, loadKey, ignoreDb, ignoreServer)
    }

    override suspend fun loadNewestMessages(conversationId: Long, replyInThread: Boolean, limit: Int,
                                            loadKey: LoadKeyData,
                                            ignoreDb: Boolean): Flow<PaginationResponse<SceytMessage>> {
        return loadMessages(LoadNewest, conversationId, 0, replyInThread, 0, limit, loadKey, ignoreDb)
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
                        val updatedMessages = saveMessagesToDb(messages)
                        messagesCache.upsertMessages(conversationId, *updatedMessages.toTypedArray())
                        checkAndMarkChannelMessagesAsDelivered(conversationId, messages)
                    }
                }
                trySend(it)
            }
        awaitClose()
    }

    override suspend fun onSyncedChannels(channels: List<SceytChannel>) {
        channels.forEach {
            if (it.messagesClearedAt > 0) {
                messageDao.deleteAllMessagesLowerThenDateIgnorePending(it.id, it.messagesClearedAt)
                messagesCache.deleteAllMessagesLowerThenDate(it.id, it.messagesClearedAt)
            }
        }
    }

    override suspend fun getMessagesByType(channelId: Long, lastMessageId: Long, type: String): SceytResponse<List<SceytMessage>> {
        val response = messagesRepository.getMessagesByType(channelId, lastMessageId, type)
        if (response is SceytResponse.Success) {
            val tIds = getMessagesTid(response.data)
            val payloads = attachmentDao.getAllAttachmentPayLoadsByMsgTid(*tIds.toLongArray())
            response.data?.forEach {
                findAndUpdateAttachmentPayLoads(it, payloads)
                it.parentMessage?.let { parent -> findAndUpdateAttachmentPayLoads(parent, payloads) }
            }
        }
        return response
    }

    override suspend fun sendMessage(channelId: Long, message: Message) {
        sendMessageAsFlow(channelId, message).collect()
    }

    override suspend fun sendMessages(channelId: Long, messages: List<Message>) {
        messages.forEach {
            sendMessageAsFlow(channelId, it).collect()
        }
    }

    override suspend fun sendMessageAsFlow(channelId: Long, message: Message): Flow<SendMessageResult> {
        val channel = channelCache.get(channelId)
                ?: persistenceChannelsLogic.getChannelFromDb(channelId)
        if (channel?.pending == true) {
            return createChannelAndSendMessageWithLock(channel, message, isPendingMessage = false, isUploadedAttachments = false).also {
                if (!createChannelAndSendMessageMutex.isLocked)
                    channelCache.removeFromPendingToRealChannelsData(channelId)
            }
        }
        return sendMessageImpl(channelId, message, isSharing = false, isPendingMessage = false, isUploadedAttachments = false)
    }

    private suspend fun createChannelAndSendMessageWithLock(channel: SceytChannel, message: Message,
                                                            isPendingMessage: Boolean, isUploadedAttachments: Boolean): Flow<SendMessageResult> {
        createChannelAndSendMessageMutex.withLock {
            val channelId = channel.id
            channel.lastMessage = message.toSceytUiMessage()
            SceytLog.i("experimentChannel", "channelId $channelId")
            channelCache.getRealChannelIdWithPendingChannelId(channelId)?.let {
                message.channelId = it
                SceytLog.i("experimentChannel", "found in cash $it")
                return sendMessageImpl(it, message, false, isPendingMessage, isUploadedAttachments)
            }

            when (val response = createNewChannelInsteadOfPendingChannel(channel)) {
                is SceytResponse.Success -> {
                    val newChannelId = response.data?.id ?: 0L
                    message.channelId = newChannelId
                    SceytLog.i("experimentChannel", "send new message: ${message.channelId}")
                    return sendMessageImpl(newChannelId, message, false, isPendingMessage, isUploadedAttachments)
                }

                is SceytResponse.Error -> {
                    channelCache.addPendingChannel(channel)
                    SceytLog.e("experimentChannel", "created new channel failed ${response.exception?.message}")

                    return callbackFlow {
                        if (!isPendingMessage && !isUploadedAttachments)
                            emitTmpMessageAndStore(channelId, message, this.channel)

                        trySend(SendMessageResult.Error(SceytResponse.Error(response.exception)))
                        this.channel.close()
                    }
                }
            }
        }
    }

    private suspend fun emitTmpMessageAndStore(channelId: Long, message: Message, sendChannel: SendChannel<SendMessageResult>) {
        val tmpMessage = tmpMessageToSceytMessage(channelId, message)
        sendChannel.trySend(SendMessageResult.TempMessage(tmpMessage))
        MessageEventsObserver.emitOutgoingMessage(tmpMessage)
        messagesCache.add(channelId, tmpMessage)
        insertTmpMessageToDb(tmpMessage)
    }

    private suspend fun createNewChannelInsteadOfPendingChannel(channel: SceytChannel): SceytResponse<SceytChannel> {
        val response = persistenceChannelsLogic.createNewChannelInsteadOfPendingChannel(channel)
        if (response is SceytResponse.Success) {
            response.data?.let { messagesCache.moveMessagesToNewChannel(channel.id, it.id) }
        }
        return response
    }

    override suspend fun sendSharedFileMessage(channelId: Long, message: Message) {
        sendMessageImpl(channelId, message, isSharing = true, isPendingMessage = false, false).collect()
    }

    override suspend fun sendFrowardMessages(channelId: Long, vararg messageToSend: Message): SceytResponse<Boolean> {
        // At first save messages to db and emit them to UI as outgoing message
        messageToSend.forEach {
            val tmpMessage = it.toSceytUiMessage().apply {
                createdAt = System.currentTimeMillis()
                user = ClientWrapper.currentUser ?: User(preference.getUserId())
            }
            MessageEventsObserver.emitOutgoingMessage(tmpMessage)
            insertTmpMessageToDb(tmpMessage)
            it.attachments?.forEach { attachment ->
                if (attachment.type != AttachmentTypeEnum.Link.value()) {
                    var state = TransferState.PendingDownload
                    var progressPercent = 0f
                    if (attachment.filePath.isNotNullOrBlank()) {
                        state = TransferState.Uploaded
                        progressPercent = 100f
                    }
                    persistenceAttachmentLogic.updateAttachmentWithTransferData(
                        TransferData(tmpMessage.tid, progressPercent, state, attachment.filePath, attachment.url))
                }
            }
            messagesCache.add(channelId, tmpMessage)
        }

        // Then send messages
        SendForwardMessagesWorkManager.schedule(context, channelId, *messageToSend.map { it.tid }.toLongArray())
        return SceytResponse.Success(true)
    }

    override suspend fun sendMessageWithUploadedAttachments(channelId: Long, message: Message): SceytResponse<SceytMessage> {
        val channel = channelCache.get(channelId)
                ?: persistenceChannelsLogic.getChannelFromDb(channelId)
        val response = if (channel?.pending == true) {
            when (val createChannelResponse = createNewChannelInsteadOfPendingChannel(channel)) {
                is SceytResponse.Success -> {
                    val newChannelId = createChannelResponse.data?.id ?: 0L
                    message.channelId = newChannelId
                    sendMessageImpl(newChannelId, message, isSharing = false,
                        isPendingMessage = false, isUploadedAttachments = true).first {
                        it.isServerResponse()
                    }.response()
                }

                is SceytResponse.Error -> SceytResponse.Error(createChannelResponse.exception)
            }
        } else {
            sendMessageImpl(channelId, message, isSharing = false,
                isPendingMessage = false, isUploadedAttachments = true).first {
                it.isServerResponse()
            }.response()
        }
        if (response is SceytResponse.Success)
            response.data?.let { persistenceAttachmentLogic.updateAttachmentIdAndMessageId(it) }

        return response
                ?: SceytResponse.Error(SceytException(0, "sendMessageWithUploadedAttachments: response is null"))
    }


    private fun sendMessageImpl(channelId: Long, message: Message, isSharing: Boolean, isPendingMessage: Boolean, isUploadedAttachments: Boolean) = callbackFlow {
        // If message is pending, we don't need to insert it to db and emit it to UI as outgoing message
        if (!isPendingMessage && !isUploadedAttachments)
            emitTmpMessageAndStore(channelId, message, this.channel)

        if (checkHasFileAttachments(message) && !isUploadedAttachments) {
            SendAttachmentWorkManager.schedule(context, message.tid, channelId, isSharing = isSharing).await()
            trySend(SendMessageResult.StartedSendingAttachment)
            channel.close()
        } else {
            messagesRepository.sendMessageAsFlow(channelId, message)
                .onCompletion { channel.close() }
                .collect { result ->
                    if (result.isServerResponse()) {
                        onMessageSentResponse(channelId, result.response(), message)
                        trySend(result)
                    }
                }
        }
        awaitClose()
    }

    private fun tmpMessageToSceytMessage(channelId: Long, message: Message): SceytMessage {
        val tmpMessage = message.toSceytUiMessage().apply {
            createdAt = System.currentTimeMillis()
            user = ClientWrapper.currentUser ?: User(preference.getUserId())
            this.channelId = channelId
            attachments?.map {
                it.transferState = TransferState.WaitingToUpload
                it.progressPercent = 0f
                if (!it.existThumb())
                    it.addAttachmentMetadata(context)
            }
        }
        return tmpMessage
    }

    private fun checkHasFileAttachments(message: Message): Boolean {
        if (message.attachments.isNullOrEmpty()) return false
        message.attachments.forEach {
            if (it.type != AttachmentTypeEnum.Link.value()) return true
        }
        return false
    }

    private suspend fun insertTmpMessageToDb(message: SceytMessage) {
        val tmpMessageDb = message.toMessageDb(false).also {
            it.messageEntity.id = null
            /*
            // todo reply in thread
            if (message.replyInThread)
                 it.messageEntity.channelId = message.parentMessage?.id ?: 0*/
        }
        messageDao.upsertMessage(tmpMessageDb)
        persistenceChannelsLogic.updateLastMessageWithLastRead(message.channelId, message)
    }

    private suspend fun onMessageSentResponse(channelId: Long, response: SceytResponse<SceytMessage>?, message: Message) {
        when (response ?: return) {
            is SceytResponse.Success -> {
                SceytLog.i(TAG, "Send message success, channel id $channelId, tid:${message.tid}, id:${message.id}")
                response.data?.let { responseMsg ->
                    messagesCache.messageUpdated(channelId, responseMsg)
                    messageDao.updateMessage(responseMsg.toMessageEntity(false))
                    persistenceChannelsLogic.updateLastMessageWithLastRead(channelId, responseMsg)
                }
            }

            is SceytResponse.Error -> {
                if ((response as? SceytResponse.Error)?.exception?.type == SDKErrorTypeEnum.BadParam.toString()) {
                    messageDao.deleteMessageByTid(message.tid)
                    SceytLog.e(TAG, "Received BadParam error: ${response.exception?.message}, " +
                            "deleting message from db channel id $channelId, tid:${message.tid} id:${message.id}")
                } else
                    SceytLog.e(TAG, "Send message error: ${response.message}, channel id $channelId, tid:${message.tid} id:${message.id}")
            }
        }
    }

    override suspend fun sendPendingMessages(channelId: Long) {
        val pendingMessages = messageDao.getPendingMessages(channelId)
        val channel = channelCache.get(channelId)
                ?: persistenceChannelsLogic.getChannelFromDb(channelId)

        if (pendingMessages.isNotEmpty()) {
            if (channel?.pending == true) {
                pendingMessages.forEach {
                    createChannelAndSendMessageWithLock(channel, it.toMessage(), isPendingMessage = true, isUploadedAttachments = false).collect()
                }
                if (!createChannelAndSendMessageMutex.isLocked)
                    channelCache.removeFromPendingToRealChannelsData(channelId)
            } else {
                pendingMessages.forEach {
                    if (it.attachments.isNullOrEmpty() || it.attachments.any { attachmentDb -> attachmentDb.payLoad?.transferState != TransferState.PauseUpload }) {
                        val message = it.toMessage()
                        sendMessageImpl(channelId, message, isSharing = false, isPendingMessage = true, isUploadedAttachments = false).collect()
                    }
                }
            }
        }
    }

    override suspend fun sendAllPendingMessages() {
        val pendingMessagesGroup = messageDao.getAllPendingMessages().groupBy { it.messageEntity.channelId }
        if (pendingMessagesGroup.isNotEmpty()) {
            pendingMessagesGroup.forEach {
                val channelId = it.key
                sendPendingMessages(channelId)
            }
        }
    }

    override suspend fun sendAllPendingMarkers() {
        val pendingMarkers = pendingMarkersDao.getAllMarkers()
        if (pendingMarkers.isNotEmpty()) {
            val groupByChannel = pendingMarkers.groupBy { it.channelId }
            for ((channelId, messages) in groupByChannel) {
                val messagesByStatus = messages.groupBy { it.name }
                for ((status, msg) in messagesByStatus)
                    markMessagesAs(channelId, status, *msg.map { it.messageId }.toLongArray())
            }
        }
    }

    override suspend fun sendAllPendingMessageStateUpdates() {
        pendingMessageStateDao.getAllWithMessage().groupBy { it.entity.channelId }.forEach {
            val channelId = it.key
            it.value.forEach values@{ stateDb ->
                when (stateDb.entity.state) {
                    MessageState.Edited -> {
                        val message = stateDb.message?.toSceytMessage()
                        if (message == null) {
                            pendingMessageStateDao.deleteByMessageId(stateDb.entity.messageId)
                            return@values
                        }
                        editMessageImpl(channelId, stateDb.message.toSceytMessage())
                    }

                    MessageState.Deleted -> {
                        deleteMessageImpl(channelId, stateDb.entity.messageId, stateDb.entity.deleteOnlyForMe)
                    }

                    else -> return@values
                }
            }
        }
    }

    override suspend fun markMessageAsDelivered(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>> {
        return markMessagesAs(channelId, Received, *ids)
    }

    override suspend fun markMessagesAsRead(channelId: Long, vararg ids: Long): List<SceytResponse<MessageListMarker>> {
        return markMessagesAs(channelId, Displayed, *ids)
    }

    override suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        suspend fun updateMessage(message: SceytMessage) {
            messageDao.upsertMessage(message.toMessageDb(false))
            messagesCache.messageUpdated(channelId, message)
            persistenceChannelsLogic.onMessageEditedOrDeleted(message)
        }

        val isPending = message.deliveryStatus == DeliveryStatus.Pending

        updateMessage(message.clone().apply {
            if (!isPending)
                state = MessageState.Edited
        })

        if (isPending)
            return SceytResponse.Success(message)

        // Insert pending message state
        pendingMessageStateDao.insert(PendingMessageStateEntity(message.id, channelId, MessageState.Edited,
            message.body, false))

        return editMessageImpl(channelId, message)
    }

    override suspend fun deleteMessage(channelId: Long, message: SceytMessage, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            val clonedMessage = message.clone().apply {
                state = MessageState.Deleted
            }
            messageDao.deleteMessageByTid(message.tid)
            messagesCache.deleteMessage(channelId, message.tid)
            persistenceChannelsLogic.onMessageEditedOrDeleted(clonedMessage)
            SendAttachmentWorkManager.cancelWorksByTag(context, message.tid.toString())
            message.attachments?.firstOrNull()?.let {
                fileTransferService.pause(it.messageTid, it, it.transferState
                        ?: TransferState.Uploading)
            }
            return SceytResponse.Success(clonedMessage)
        }

        // Insert pending message state
        pendingMessageStateDao.insert(PendingMessageStateEntity(message.id, channelId, MessageState.Deleted, null, onlyForMe))

        // Update message state in db and cache
        val deletedMessage = message.clone().apply { state = MessageState.Deleted }
        onMessageEditedOrDeleted(deletedMessage)
        persistenceChannelsLogic.onMessageEditedOrDeleted(deletedMessage)

        return deleteMessageImpl(channelId, message.id, onlyForMe)
    }

    private suspend fun editMessageImpl(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> {
        val response = messagesRepository.editMessage(channelId, message)
        if (response is SceytResponse.Success) {
            response.data?.let { updatedMsg ->
                pendingMessageStateDao.deleteByMessageId(updatedMsg.id)
            }
        }
        return response
    }

    private suspend fun deleteMessageImpl(channelId: Long, messageId: Long, onlyForMe: Boolean): SceytResponse<SceytMessage> {
        val response = messagesRepository.deleteMessage(channelId, messageId, onlyForMe)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                pendingMessageStateDao.deleteByMessageId(resultMessage.id)
            }
        }
        return response
    }

    override suspend fun getMessageFromServerById(channelId: Long, messageId: Long): SceytResponse<SceytMessage> {
        val result = messagesRepository.getMessageById(channelId, messageId)
        if (result is SceytResponse.Success) {
            result.data?.let { message ->
                messageDao.insertMessageIgnored(message.toMessageDb(true))
            }
        }
        return result
    }

    override suspend fun getMessageDbById(messageId: Long): SceytMessage? {
        return messageDao.getMessageById(messageId)?.toSceytMessage()
    }

    override suspend fun getMessageDbByTid(tid: Long): SceytMessage? {
        return messageDao.getMessageByTid(tid)?.toSceytMessage()
    }

    override suspend fun getMessagesDbByTid(tIds: List<Long>): List<SceytMessage> {
        return messageDao.getMessagesByTid(tIds).map { it.toSceytMessage() }
    }

    override suspend fun attachmentSuccessfullySent(message: SceytMessage) {
        messageDao.upsertMessage(message.toMessageDb(false))
        messagesCache.upsertNotifyUpdateAnyway(message.channelId, message)
    }

    override suspend fun saveChannelLastMessagesToDb(list: List<SceytMessage>?) {
        saveMessagesToDb(list)
    }

    override fun getOnMessageFlow() = onMessageFlow.asSharedFlow()


    private fun loadMessages(loadType: LoadType, conversationId: Long, messageId: Long,
                             replyInThread: Boolean, offset: Int, limit: Int,
                             loadKey: LoadKeyData = LoadKeyData(value = messageId),
                             ignoreDb: Boolean, ignoreServer: Boolean = false): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            if (offset == 0) messagesCache.clear()

            var dbResultWasEmpty = true
            // Load from database
            if (!ignoreDb) {
                trySend(getMessagesDbByLoadType(loadType, conversationId, messageId, offset, limit, loadKey).also {
                    dbResultWasEmpty = it.data.isEmpty()
                })
            }
            // Load from server
            if (!ignoreServer)
                trySend(getMessagesServerByLoadType(loadType, conversationId, messageId, offset,
                    limit, replyInThread, loadKey, ignoreDb, dbResultWasEmpty))

            channel.close()
            awaitClose()
        }
    }

    private suspend fun markMessagesAsDelivered(channelId: Long, messages: List<SceytMessage>) {
        // Mark messages as received
        withContext(Dispatchers.IO) {
            checkAndMarkChannelMessagesAsDelivered(channelId, messages)
        }
    }

    private suspend fun getMessagesDbByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                offset: Int, limit: Int, loadKey: LoadKeyData): PaginationResponse.DBResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        val messages: List<SceytMessage>

        when (loadType) {
            LoadPrev -> {
                messages = getPrevMessagesDb(channelId, lastMessageId, offset, limit)
                hasPrev = messages.size == MESSAGES_LOAD_SIZE
            }

            LoadNext -> {
                messages = getNextMessagesDb(channelId, lastMessageId, offset, limit)
                hasNext = messages.size == MESSAGES_LOAD_SIZE
            }

            LoadNear -> {
                val data = getNearMessagesDb(channelId, lastMessageId, offset, limit)
                messages = data.data.map { it.toSceytMessage() }
                hasPrev = data.hasPrev
                hasNext = data.hasNext
            }

            LoadNewest -> {
                messages = getPrevMessagesDb(channelId, Long.MAX_VALUE, offset, limit)
                hasPrev = messages.size == MESSAGES_LOAD_SIZE
            }
        }

        messagesCache.addAll(channelId, messages, false)

        // Mark messages as received
        markMessagesAsDelivered(channelId, messages)

        return PaginationResponse.DBResponse(messages, loadKey, offset, hasNext, hasPrev, loadType)
    }

    private suspend fun getMessagesServerByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                    offset: Int, limit: Int, replyInThread: Boolean,
                                                    loadKey: LoadKeyData = LoadKeyData(value = lastMessageId),
                                                    ignoreDb: Boolean, dbResultWasEmpty: Boolean): PaginationResponse.ServerResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        var hasDiff: Boolean
        var forceHasDiff = false
        var messages: List<SceytMessage> = emptyList()
        val response: SceytResponse<List<SceytMessage>>

        ConnectionEventsObserver.awaitToConnectSceyt()

        when (loadType) {
            LoadPrev -> {
                response = messagesRepository.getPrevMessages(channelId, lastMessageId, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
                    // Check maybe messages was cleared
                    if (offset == 0 && messages.isEmpty()) {
                        messageDao.deleteAllMessagesExceptPending(channelId)
                        messagesCache.clearAllExceptPending(channelId)
                        forceHasDiff = true
                    }
                }
            }

            LoadNext -> {
                response = messagesRepository.getNextMessages(channelId, lastMessageId, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasNext = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }

            LoadNear -> {
                response = messagesRepository.getNearMessages(channelId, lastMessageId, replyInThread, limit)
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
                response = messagesRepository.getPrevMessages(channelId, Long.MAX_VALUE, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
                }
            }
        }

        val updatedMessages = saveMessagesToDb(messages)
        hasDiff = messagesCache.addAll(channelId, updatedMessages, true)

        if (forceHasDiff) hasDiff = true

        // Mark messages as received
        markMessagesAsDelivered(channelId, messages)

        return PaginationResponse.ServerResponse(
            data = response, cacheData = messagesCache.getSorted(channelId),
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb, dbResultWasEmpty = dbResultWasEmpty)
    }

    private fun findAndUpdateAttachmentPayLoads(message: SceytMessage, payloads: List<AttachmentPayLoadEntity>) {
        payloads.filter { payLoad -> payLoad.messageTid == message.tid }.let { entity ->
            message.attachments?.forEach { attachment ->
                val predicate: (AttachmentPayLoadEntity) -> Boolean = if (attachment.url.isNotNullOrBlank()) {
                    { entity.any { it.url == attachment.url } }
                } else {
                    { entity.any { it.filePath == attachment.filePath } }
                }

                payloads.find(predicate)?.let {
                    attachment.transferState = it.transferState
                    attachment.progressPercent = it.progressPercent
                    attachment.filePath = it.filePath
                    attachment.url = it.url
                }
            }
        }
    }

    private suspend fun getPrevMessagesDb(channelId: Long, lastMessageId: Long, offset: Int, limit: Int): List<SceytMessage> {
        var lastMsgId = lastMessageId
        if (lastMessageId == 0L)
            lastMsgId = Long.MAX_VALUE

        var messages = messageDao.getOldestThenMessages(channelId, lastMsgId, limit).reversed()

        if (offset == 0)
            messages = getPendingMessagesAndAddToList(channelId, messages.toArrayList())

        return messages.map { messageDb -> messageDb.toSceytMessage() }
    }

    private suspend fun getNextMessagesDb(channelId: Long, lastMessageId: Long, offset: Int, limit: Int): List<SceytMessage> {
        var messages = messageDao.getNewestThenMessage(channelId, lastMessageId, limit)

        if (offset == 0)
            messages = getPendingMessagesAndAddToList(channelId, messages.toArrayList())

        return messages.map { messageDb -> messageDb.toSceytMessage() }
    }

    private suspend fun getNearMessagesDb(channelId: Long, messageId: Long, offset: Int, limit: Int): LoadNearData<MessageDb> {
        val data = messageDao.getNearMessages(channelId, messageId, limit)
        val messages = data.data

        if (offset == 0)
            data.data = getPendingMessagesAndAddToList(channelId, messages.toArrayList())

        return data
    }

    private suspend fun getPendingMessagesAndAddToList(channelId: Long, list: ArrayList<MessageDb>): List<MessageDb> {
        val pendingMessage = messageDao.getPendingMessages(channelId)

        return if (pendingMessage.isNotEmpty()) {
            list.addAll(pendingMessage)
            list.sortedBy { it.messageEntity.createdAt }
        } else list
    }

    private suspend fun saveMessagesToDb(list: List<SceytMessage>?, includeParents: Boolean = true): List<SceytMessage> {
        if (list.isNullOrEmpty()) return emptyList()
        val pendingStates = pendingMessageStateDao.getAll()
        val usersDb = arrayListOf<UserEntity>()
        val messagesDb = arrayListOf<MessageDb>()
        val parentMessagesDb = arrayListOf<MessageDb>()

        for (message in list) {
            updateMessageStatesWithPendingStates(message, pendingStates)
            messagesDb.add(message.toMessageDb(false))
            if (includeParents) {
                message.parentMessage?.let { parent ->
                    if (parent.id != 0L) {
                        parentMessagesDb.add(parent.toMessageDb(true))
                        if (parent.incoming)
                            parent.user?.let { user -> usersDb.add(user.toUserEntity()) }
                    }
                }
            }
        }
        messageDao.upsertMessages(messagesDb)
        if (parentMessagesDb.isNotEmpty())
            messageDao.insertMessagesIgnored(parentMessagesDb)

        // Update users
        list.filter { it.incoming && it.user != null }.map { it.user!! }.toSet().let { users ->
            if (users.isNotEmpty())
                usersDb.addAll(users.map { it.toUserEntity() })
        }

        userDao.insertUsers(usersDb)

        return list
    }

    private fun updateMessageStatesWithPendingStates(message: SceytMessage, pendingStates: List<PendingMessageStateEntity>) {
        pendingStates.find { it.messageId == message.id }?.let {
            message.state = it.state
            if (it.state == MessageState.Edited && !it.editBody.isNullOrBlank())
                message.body = it.editBody
        }
    }

    private suspend fun deletedPayloads(id: Long, tid: Long) {
        messageDao.deleteAttachmentsChunked(listOf(tid))
        messageDao.deleteAttachmentsPayloadsChunked(listOf(tid))
        reactionDao.deleteAllReactionsAndTotals(id)
    }

    private fun getMessagesTid(messages: List<SceytMessage>?): List<Long> {
        val tIds = mutableListOf<Long>()
        messages?.forEach {
            tIds.add(it.tid)
            it.parentMessage?.let { parent -> tIds.add(parent.tid) }
        }
        return tIds
    }

    private suspend fun checkAndMarkChannelMessagesAsDelivered(channelId: Long, messages: List<SceytMessage>) {
        val notDisplayedMessages = messages.filter {
            it.incoming && it.userMarkers?.any { marker -> marker.name == Received.value() } != true
        }
        if (notDisplayedMessages.isNotEmpty())
            markMessageAsDelivered(channelId, *notDisplayedMessages.map { it.id }.toLongArray())
    }

    private suspend fun markMessagesAs(channelId: Long, status: MarkerTypeEnum, vararg ids: Long): List<SceytResponse<MessageListMarker>> {
        val responseList = mutableListOf<SceytResponse<MessageListMarker>>()
        ids.toList().chunked(50).forEach {
            val typedArray = it.toLongArray()
            addPendingMarkerToDb(channelId, status, *typedArray)

            val response = if (status == Displayed)
                messagesRepository.markAsDisplayed(channelId, *typedArray)
            else messagesRepository.markAsReceived(channelId, *typedArray)

            onMarkerResponse(channelId, response, status, *typedArray)
            responseList.add(response)
        }

        return responseList
    }

    private suspend fun addPendingMarkerToDb(channelId: Long, status: MarkerTypeEnum, vararg ids: Long) {
        if (ids.isEmpty()) return
        val existMessageIds = messageDao.getExistMessageByIds(ids.toList())
        if (existMessageIds.isEmpty()) return
        val list = existMessageIds.map { PendingMarkerEntity(channelId = channelId, messageId = it, name = status) }
        pendingMarkersDao.insertMany(list)
    }

    private suspend fun onMarkerResponse(channelId: Long, response: SceytResponse<MessageListMarker>, status: MarkerTypeEnum, vararg ids: Long) {
        when (response) {
            is SceytResponse.Success -> {
                response.data?.let { data ->
                    val deliveryStatus = status.toDeliveryStatus()
                    messageDao.updateMessagesStatus(channelId, data.messageIds, deliveryStatus)
                    val tIds = messageDao.getMessageTIdsByIds(*ids)
                    messagesCache.updateMessagesStatus(channelId, deliveryStatus, *tIds.toLongArray())

                    pendingMarkersDao.deleteMessagesMarkersByStatus(ids.toList(), status)
                    val existMessageIds = messageDao.getExistMessageByIds(ids.toList())
                    existMessageIds.forEach {
                        SceytKitClient.myId?.let { userId ->
                            val markerEntity = MarkerEntity(messageId = it, userId = userId, name = data.name)
                            messageDao.insertUserMarker(markerEntity)
                        }
                    }
                }
            }

            is SceytResponse.Error -> {
                // Check if error code is 1301 (not allowed) then delete pending markers
                if (response.exception?.code == 1301)
                    pendingMarkersDao.deleteMessagesMarkersByStatus(ids.toList(), status)
            }
        }
    }
}