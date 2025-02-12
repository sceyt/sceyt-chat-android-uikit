package com.sceyt.chatuikit.persistence.logicimpl.message

import android.content.Context
import androidx.work.await
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chat.models.message.DeleteMessageType.DeleteForEveryone
import com.sceyt.chat.models.message.DeleteMessageType.DeleteForMe
import com.sceyt.chat.models.message.DeleteMessageType.DeleteHard
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.message.MessageEventManager
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventData
import com.sceyt.chatuikit.data.managers.message.event.ReactionUpdateEventEnum
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytPagingResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.SendMessageResult
import com.sceyt.chatuikit.data.models.SendMessageResult.Companion.toSendMessageResult
import com.sceyt.chatuikit.data.models.SyncNearMessagesResult
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.MarkerType.Received
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.repositories.getUserId
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.toDeliveryStatus
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.NotificationType
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.AutoDeleteMessageDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMarkerDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageStateDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageStateEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.addAttachmentMetadata
import com.sceyt.chatuikit.persistence.mappers.existThumb
import com.sceyt.chatuikit.persistence.mappers.getLinkPreviewDetails
import com.sceyt.chatuikit.persistence.mappers.isHiddenLinkDetails
import com.sceyt.chatuikit.persistence.mappers.isLink
import com.sceyt.chatuikit.persistence.mappers.toLinkPreviewDetails
import com.sceyt.chatuikit.persistence.mappers.toMessage
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toMessageEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.workers.SendForwardMessagesWorkManager
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import com.sceyt.chatuikit.push.PushData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

internal class PersistenceMessagesLogicImpl(
        private val context: Context,
        private val messageDao: MessageDao,
        private val autoDeleteMessageDao: AutoDeleteMessageDao,
        private val rangeDao: LoadRangeDao,
        private val attachmentDao: AttachmentDao,
        private val pendingMarkerDao: PendingMarkerDao,
        private val reactionDao: ReactionDao,
        private val userDao: UserDao,
        private val pendingMessageStateDao: PendingMessageStateDao,
        private val fileTransferService: FileTransferService,
        private val messagesRepository: MessagesRepository,
        private val preference: SceytSharedPreference,
        private val messagesCache: MessagesCache,
        private val channelCache: ChannelsCache,
        private val messageLoadRangeUpdater: MessageLoadRangeUpdater,
) : PersistenceMessagesLogic, SceytKoinComponent {

    private val persistenceChannelsLogic: PersistenceChannelsLogic by inject()
    private val persistenceAttachmentLogic: PersistenceAttachmentLogic by inject()
    private val persistenceReactionLogic: PersistenceReactionsLogic by inject()
    private val createChannelAndSendMessageMutex = Mutex()
    private val dispatcherIO = Dispatchers.IO
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId
    private val messagesLoadSize get() = SceytChatUIKit.config.queryLimits.messageListQueryLimit

    private val onMessageFlow: MutableSharedFlow<Pair<SceytChannel, SceytMessage>> = MutableSharedFlow(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun onMessage(
            data: Pair<SceytChannel, SceytMessage>,
            sendDeliveryMarker: Boolean,
    ): Unit = withContext(dispatcherIO) {
        val channel = data.first
        val message = data.second
        saveMessagesToDb(arrayListOf(message))

        messagesCache.add(channel.id, message)
        onMessageFlow.tryEmit(data)
        updateMessageLoadRangeOnMessageEvent(message, null)

        launch {
            if (message.incoming && sendDeliveryMarker)
                markMessagesAs(channel.id, Received, message.id)
        }
    }

    override suspend fun handlePush(data: PushData): Boolean = withContext(dispatcherIO) {
        val message = data.message
        if (message.id == 0L)
            return@withContext false
        val channel = persistenceChannelsLogic.getChannelFromDb(data.channel.id)
        if (channel != null && message.createdAt <= channel.messagesClearedAt)
            return@withContext false

        val messageDb = messageDao.getMessageById(message.id)
        val isReaction = data.type == NotificationType.MessageReaction

        if (messageDb == null && !isReaction) {
            saveMessagesToDb(arrayListOf(message), includeParents = false, replaceUserOnConflict = false)
            messagesCache.add(data.channel.id, message)
            onMessageFlow.tryEmit(Pair(data.channel, message))

            updateMessageLoadRangeOnMessageEvent(message, channel?.lastMessage?.id)
            persistenceChannelsLogic.handlePush(data)
        }

        if (messageDb != null && isReaction)
            persistenceReactionLogic.onMessageReactionUpdated(ReactionUpdateEventData(
                messageDb.toSceytMessage(), data.reaction!!, ReactionUpdateEventEnum.Add))

        return@withContext true
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) = withContext(dispatcherIO) {
        val updatedMessages = messageDao.updateMessageStatusWithBefore(data.channel.id, data.status, data.marker.messageIds.maxOf { it })
        messagesCache.updateMessagesStatus(data.channel.id, data.status, *updatedMessages.map { it.tid }.toLongArray())
    }

    override suspend fun onMessageEditedOrDeleted(message: SceytMessage) = withContext(dispatcherIO) {
        when (message.state) {
            MessageState.Unmodified -> return@withContext
            MessageState.Edited, MessageState.Moderated -> {
                val selfReactions = reactionDao.getSelfReactionsByMessageId(message.id, myId.toString())
                val updateMsg = message.copy(userReactions = selfReactions.map { it.toSceytReaction() })
                messagesCache.messageUpdated(updateMsg.channelId, updateMsg)
                messageDao.updateMessage(updateMsg.toMessageEntity(false))
            }

            MessageState.Deleted -> {
                messagesCache.messageUpdated(message.channelId, message)
                messageDao.updateMessage(message.toMessageEntity(false))
                deletedPayloads(message.id, message.tid)
            }

            MessageState.DeletedHard -> {
                messageDao.deleteMessageByTid(message.tid)
                messagesCache.hardDeleteMessage(message.channelId, message)
            }
        }
    }

    override suspend fun loadPrevMessages(
            conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
            offset: Int, limit: Int, loadKey: LoadKeyData,
            ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> = withContext(dispatcherIO) {
        return@withContext loadMessages(LoadPrev, conversationId, lastMessageId, replyInThread, offset, limit, loadKey, ignoreDb)
    }

    override suspend fun loadNextMessages(
            conversationId: Long, lastMessageId: Long, replyInThread: Boolean,
            offset: Int, limit: Int,
            ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> = withContext(dispatcherIO) {
        return@withContext loadMessages(LoadNext, conversationId, lastMessageId, replyInThread, offset, limit, ignoreDb = ignoreDb)
    }

    override suspend fun loadNearMessages(
            conversationId: Long, messageId: Long, replyInThread: Boolean,
            limit: Int, loadKey: LoadKeyData, ignoreDb: Boolean,
            ignoreServer: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> = withContext(dispatcherIO) {
        return@withContext loadMessages(LoadNear, conversationId, messageId, replyInThread, 0, limit, loadKey, ignoreDb, ignoreServer)
    }

    override suspend fun loadNewestMessages(
            conversationId: Long, replyInThread: Boolean, limit: Int,
            loadKey: LoadKeyData,
            ignoreDb: Boolean,
    ): Flow<PaginationResponse<SceytMessage>> = withContext(dispatcherIO) {
        return@withContext loadMessages(LoadNewest, conversationId, 0, replyInThread, 0, limit, loadKey, ignoreDb)
    }

    override suspend fun searchMessages(
            conversationId: Long, replyInThread: Boolean,
            query: String,
    ): SceytPagingResponse<List<SceytMessage>> = withContext(dispatcherIO) {
        return@withContext messagesRepository.searchMessages(conversationId, replyInThread, query)
    }

    override suspend fun loadNextSearchMessages(): SceytPagingResponse<List<SceytMessage>> = withContext(dispatcherIO) {
        return@withContext messagesRepository.loadNextSearchMessages()
    }

    override suspend fun loadMessagesById(conversationId: Long, ids: List<Long>): SceytResponse<List<SceytMessage>> = withContext(dispatcherIO) {
        val response = messagesRepository.loadMessagesById(conversationId, ids)
        if (response is SceytResponse.Success)
            saveMessagesToDb(response.data, unListAll = true)
        return@withContext response
    }

    override suspend fun syncMessagesAfterMessageId(
            conversationId: Long, replyInThread: Boolean,
            messageId: Long,
    ): Flow<SceytResponse<List<SceytMessage>>> = callbackFlow {
        ConnectionEventManager.awaitToConnectSceyt()
        messagesRepository.loadAllMessagesAfter(conversationId, replyInThread, messageId)
            .onCompletion { channel.close() }
            .collect { (nextMessageId, response) ->
                if (response is SceytResponse.Success) {
                    response.data?.let { messages ->
                        val updatedMessages = saveMessagesToDb(messages)
                        messagesCache.upsertMessages(conversationId, *updatedMessages.toTypedArray())
                        checkAndMarkChannelMessagesAsDelivered(conversationId, messages)
                        updateMessageLoadRange(messageId = nextMessageId, channelId = conversationId, response)
                    }
                }
                trySend(response)
            }
        awaitClose()
    }

    override suspend fun syncNearMessages(
            conversationId: Long, messageId: Long,
            replyInThread: Boolean,
    ): SyncNearMessagesResult = withContext(dispatcherIO) {

        val response = messagesRepository.getNearMessages(conversationId, messageId, replyInThread, 30)
        var missingMessages = emptyList<SceytMessage>()
        if (response is SceytResponse.Success) {
            val messages = response.data
            val updatedMessages = saveMessagesToDb(messages)
            missingMessages = messagesCache.updateAllSyncedMessagesAndGetMissing(conversationId, updatedMessages)
            updateMessageLoadRange(messageId = messageId, channelId = conversationId, response = response)
        }
        return@withContext SyncNearMessagesResult(messageId, response, missingMessages)
    }

    override suspend fun onSyncedChannels(channels: List<SceytChannel>) = withContext(dispatcherIO) {
        channels.forEach {
            if (it.messagesClearedAt > 0) {
                messageDao.deleteAllMessagesLowerThenDateIgnorePending(it.id, it.messagesClearedAt)
                messagesCache.deleteAllMessagesLowerThenDate(it.id, it.messagesClearedAt)
            }
        }
    }

    override suspend fun getMessagesByType(
            channelId: Long, lastMessageId: Long,
            type: String,
    ): SceytResponse<List<SceytMessage>> = withContext(dispatcherIO) {
        val response = messagesRepository.getMessagesByType(channelId, lastMessageId, type)
        if (response is SceytResponse.Success) {
            val tIds = getMessagesTid(response.data)
            val payloads = attachmentDao.getAllAttachmentPayLoadsByMsgTid(*tIds.toLongArray())
            return@withContext SceytResponse.Success(response.data?.map {
                val parentMsg = it.parentMessage?.let { message -> findAndUpdateAttachmentPayLoads(message, payloads) }
                val msg = findAndUpdateAttachmentPayLoads(it, payloads)
                msg.copy(parentMessage = parentMsg)
            })
        }
        return@withContext response
    }

    override suspend fun sendMessage(channelId: Long, message: Message) {
        sendMessageAsFlow(channelId, message).collect()
    }

    override suspend fun sendMessages(channelId: Long, messages: List<Message>) {
        messages.forEach {
            sendMessageAsFlow(channelId, it).collect()
        }
    }

    override suspend fun sendMessageAsFlow(
            channelId: Long,
            message: Message,
    ): Flow<SendMessageResult> = withContext(dispatcherIO) {
        val channel = channelCache.getOneOf(channelId)
                ?: persistenceChannelsLogic.getChannelFromDb(channelId)
        if (channel?.pending == true) {
            return@withContext createChannelAndSendMessageWithLock(channel, message,
                isPendingMessage = false, isUploadedAttachments = false).also {
                if (!createChannelAndSendMessageMutex.isLocked)
                    channelCache.removeFromPendingToRealChannelsData(channelId)
            }
        }
        return@withContext sendMessageImpl(
            channelId = channelId,
            message = message,
            isSharing = false,
            isPendingMessage = false,
            isUploadedAttachments = false
        )
    }

    private suspend fun createChannelAndSendMessageWithLock(
            pendingChannel: SceytChannel,
            message: Message,
            isPendingMessage: Boolean,
            isUploadedAttachments: Boolean,
    ): Flow<SendMessageResult> {
        createChannelAndSendMessageMutex.withLock {
            val channelId = pendingChannel.id
            val updated = pendingChannel.copy(lastMessage = message.toSceytUiMessage())
            // If channel is already created, we don't need to create it again
            channelCache.getRealChannelIdWithPendingChannelId(channelId)?.let {
                message.channelId = it
                return sendMessageImpl(it, message, false, isPendingMessage, isUploadedAttachments)
            }

            if (!isPendingMessage && !isUploadedAttachments)
                emitTmpMessageAndStore(channelId, message)

            when (val response = createNewChannelInsteadOfPendingChannel(updated)) {
                is SceytResponse.Success -> {
                    val newChannelId = response.data?.id ?: 0L
                    message.channelId = newChannelId
                    return sendMessageImpl(
                        channelId = newChannelId,
                        message = message,
                        isSharing = false,
                        isPendingMessage = isPendingMessage,
                        isUploadedAttachments = isUploadedAttachments,
                        emitTmpMessageAndStore = false
                    )
                }

                is SceytResponse.Error -> {
                    channelCache.addPendingChannel(updated)
                    return callbackFlow {
                        trySend(SendMessageResult.Error(SceytResponse.Error(response.exception)))
                        this.channel.close()
                    }
                }
            }
        }
    }

    private suspend fun emitTmpMessageAndStore(channelId: Long, message: Message) {
        val tmpMessage = tmpMessageToSceytMessage(channelId, message)
        MessageEventManager.emitOutgoingMessage(tmpMessage)
        messagesCache.add(channelId, tmpMessage)
        insertTmpMessageToDb(tmpMessage)
    }

    private suspend fun createNewChannelInsteadOfPendingChannel(
            pendingChannel: SceytChannel
    ): SceytResponse<SceytChannel> {
        val response = persistenceChannelsLogic.createNewChannelInsteadOfPendingChannel(pendingChannel)
        if (response is SceytResponse.Success) {
            response.data?.let {
                messagesCache.moveMessagesToNewChannel(pendingChannel.id, it.id)
            }
        }
        return response
    }

    override suspend fun sendSharedFileMessage(channelId: Long, message: Message) = withContext(dispatcherIO) {
        sendMessageImpl(channelId, message, isSharing = true, isPendingMessage = false, false).collect()
    }

    override suspend fun sendFrowardMessages(channelId: Long, vararg messageToSend: Message): SceytResponse<Boolean> = withContext(dispatcherIO) {
        // At first save messages to db and emit them to UI as outgoing message
        messageToSend.forEach {
            val tmpMessage = it.toSceytUiMessage().copy(
                createdAt = System.currentTimeMillis(),
                user = ClientWrapper.currentUser?.toSceytUser() ?: SceytUser(preference.getUserId()
                        ?: ""),
            )
            MessageEventManager.emitOutgoingMessage(tmpMessage)
            insertTmpMessageToDb(tmpMessage)
            it.attachments?.forEach { attachment ->
                if (attachment.type != AttachmentTypeEnum.Link.value) {
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
        return@withContext SceytResponse.Success(true)
    }

    override suspend fun sendMessageWithUploadedAttachments(
            channelId: Long,
            message: Message
    ): SceytResponse<SceytMessage> = withContext(dispatcherIO) {
        val channel = channelCache.getOneOf(channelId)
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

        return@withContext response
                ?: SceytResponse.Error(SceytException(0, "sendMessageWithUploadedAttachments: response is null"))
    }

    private fun sendMessageImpl(
            channelId: Long,
            message: Message,
            isSharing: Boolean,
            isPendingMessage: Boolean,
            isUploadedAttachments: Boolean,
            emitTmpMessageAndStore: Boolean = true,
    ) = callbackFlow {
        // If message is pending, we don't need to insert it to db and emit it to UI as outgoing message
        if (!isPendingMessage && !isUploadedAttachments && emitTmpMessageAndStore)
            emitTmpMessageAndStore(channelId, message)

        if (checkHasFileAttachments(message) && !isUploadedAttachments) {
            UploadAndSendAttachmentWorkManager.schedule(context, message.tid, channelId, isSharing = isSharing).await()
            trySend(SendMessageResult.StartedSendingAttachment)
        } else {
            val response = messagesRepository.sendMessage(channelId, message)
            onMessageSentResponse(channelId, response, message)
            trySend(response.toSendMessageResult())
        }
        channel.close()
        awaitClose()
    }

    private fun tmpMessageToSceytMessage(channelId: Long, message: Message): SceytMessage {
        val sceytMessage = message.toSceytUiMessage()
        val tmpMessage = sceytMessage.copy(
            createdAt = System.currentTimeMillis(),
            user = ClientWrapper.currentUser?.toSceytUser() ?: SceytUser(preference.getUserId()
                    ?: ""),
            channelId = channelId,
            attachments = sceytMessage.attachments?.map {
                val isLink = it.isLink()
                var metadata = it.metadata
                var linkPreviewDetails = it.linkPreviewDetails
                if (!it.existThumb() && !isLink)
                    metadata = it.addAttachmentMetadata(context)
                else if (isLink)
                    linkPreviewDetails = it.getLinkPreviewDetails()

                it.copy(
                    transferState = TransferState.WaitingToUpload,
                    progressPercent = 0f,
                    metadata = metadata,
                    linkPreviewDetails = linkPreviewDetails
                )
            }
        )
        return tmpMessage
    }

    private fun checkHasFileAttachments(message: Message): Boolean {
        if (message.attachments.isNullOrEmpty()) return false
        message.attachments.forEach {
            if (it.type != AttachmentTypeEnum.Link.value) return true
        }
        return false
    }

    private suspend fun insertTmpMessageToDb(message: SceytMessage) {
        val tmpMessageDb = message.copy(id = 0).toMessageDb(unList = false)/*.also {
            // todo reply in thread
            if (message.replyInThread)
                 it.messageEntity.channelId = message.parentMessage?.id ?: 0
        }*/
        messageDao.upsertMessage(tmpMessageDb)
        persistenceChannelsLogic.updateLastMessageWithLastRead(message.channelId, message)
    }

    private suspend fun onMessageSentResponse(channelId: Long, response: SceytResponse<SceytMessage>?, message: Message) {
        when (response ?: return) {
            is SceytResponse.Success -> {
                SceytLog.i(TAG, "Send message success, channel id $channelId, tid:${message.tid}," +
                        "responseMsgTid ${response.data?.tid} id:${response.data?.id}")
                response.data?.let { responseMsg ->
                    messagesCache.messageUpdated(channelId, responseMsg)

                    val lastSentMessageId = messageDao.getLastSentMessageId(channelId)
                    updateMessageLoadRangeOnMessageEvent(responseMsg, lastSentMessageId
                            ?: responseMsg.id)

                    messageDao.upsertMessage(responseMsg.toMessageDb(false))
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

    override suspend fun sendPendingMessages(channelId: Long) = withContext(dispatcherIO) {
        val pendingMessages = messageDao.getPendingMessages(channelId)
        val channel = channelCache.getOneOf(channelId)
                ?: persistenceChannelsLogic.getChannelFromDb(channelId)

        if (pendingMessages.isNotEmpty()) {
            if (channel?.pending == true) {
                pendingMessages.forEach {
                    createChannelAndSendMessageWithLock(
                        pendingChannel = channel,
                        message = it.toMessage(),
                        isPendingMessage = true,
                        isUploadedAttachments = false
                    ).collect()
                }
                if (!createChannelAndSendMessageMutex.isLocked)
                    channelCache.removeFromPendingToRealChannelsData(channelId)
            } else {
                pendingMessages.forEach {
                    // If have attachments, we need to check maybe the upload was paused,
                    // if so, we don't need to send message until upload is finished
                    if (it.attachments.isNullOrEmpty() || it.attachments.any { attachment ->
                                attachment.payLoad?.transferState != TransferState.PauseUpload
                            }) {
                        val isUploaded = it.attachments?.all { attachment ->
                            attachment.attachmentEntity.type == AttachmentTypeEnum.Link.value
                                    || attachment.payLoad?.transferState == TransferState.Uploaded
                        } ?: false
                        val message = it.toMessage()
                        sendMessageImpl(
                            channelId = channelId,
                            message = message,
                            isSharing = false,
                            isPendingMessage = true,
                            isUploadedAttachments = isUploaded
                        ).collect()
                    }
                }
            }
        }
    }

    override suspend fun sendAllPendingMessages() = withContext(dispatcherIO) {
        val pendingMessagesGroup = messageDao.getAllPendingMessages().groupBy { it.messageEntity.channelId }
        if (pendingMessagesGroup.isNotEmpty()) {
            pendingMessagesGroup.forEach {
                val channelId = it.key
                sendPendingMessages(channelId)
            }
        }
    }

    override suspend fun sendAllPendingMarkers() = withContext(dispatcherIO) {
        val pendingMarkers = pendingMarkerDao.getAllMarkers()
        if (pendingMarkers.isNotEmpty()) {
            val groupByChannel = pendingMarkers.groupBy { it.channelId }
            for ((channelId, messages) in groupByChannel) {
                val messagesByStatus = messages.groupBy { it.name }
                for ((status, msg) in messagesByStatus)
                    addMessagesMarkerImpl(channelId, status, false, *msg.map { it.messageId }.toLongArray())
            }
        }
    }

    override suspend fun sendAllPendingMessageStateUpdates() = withContext(dispatcherIO) {
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
                        val type = if (stateDb.entity.deleteOnlyForMe) DeleteForMe else DeleteForEveryone
                        deleteMessageImpl(channelId, stateDb.entity.messageId, type)
                    }

                    MessageState.DeletedHard -> {
                        deleteMessageImpl(channelId, stateDb.entity.messageId, DeleteHard)
                    }

                    else -> return@values
                }
            }
        }
    }

    override suspend fun markMessagesAs(
            channelId: Long, marker: MarkerType,
            vararg ids: Long,
    ): List<SceytResponse<MessageListMarker>> {
        return markMessagesAsImpl(channelId, marker, *ids)
    }

    override suspend fun addMessagesMarker(channelId: Long, marker: String, vararg ids: Long): List<SceytResponse<MessageListMarker>> {
        return addMessagesMarkerImpl(channelId, marker, true, *ids)
    }

    override suspend fun editMessage(channelId: Long, message: SceytMessage): SceytResponse<SceytMessage> = withContext(dispatcherIO) {
        suspend fun updateMessage(message: SceytMessage) {
            messageDao.upsertMessage(message.toMessageDb(false))
            messagesCache.messageUpdated(channelId, message)
            persistenceChannelsLogic.onMessageEditedOrDeleted(message)
        }

        val isPending = message.deliveryStatus == DeliveryStatus.Pending

        updateMessage(message.copy(state = if (!isPending)
            MessageState.Edited else message.state))

        if (isPending)
            return@withContext SceytResponse.Success(message)

        // Insert pending message state
        pendingMessageStateDao.insert(PendingMessageStateEntity(message.id, channelId, MessageState.Edited,
            message.body, false))

        return@withContext editMessageImpl(channelId, message)
    }

    override suspend fun deleteMessage(
            channelId: Long, message: SceytMessage,
            deleteType: DeleteMessageType,
    ): SceytResponse<SceytMessage> = withContext(dispatcherIO) {
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            val clonedMessage = message.copy(state = MessageState.Deleted)
            messageDao.deleteMessageByTid(message.tid)
            messagesCache.hardDeleteMessage(channelId, message)
            persistenceChannelsLogic.onMessageEditedOrDeleted(clonedMessage)
            UploadAndSendAttachmentWorkManager.cancelWorksByTag(context, message.tid.toString())
            message.attachments?.firstOrNull()?.let {
                fileTransferService.pause(it.messageTid, it, it.transferState
                        ?: TransferState.Uploading)
            }
            return@withContext SceytResponse.Success(clonedMessage)
        }

        // Insert pending message state
        val onlyForMe: Boolean
        val state: MessageState
        when (deleteType) {
            DeleteHard -> {
                onlyForMe = false
                state = MessageState.DeletedHard
            }

            DeleteForEveryone -> {
                onlyForMe = false
                state = MessageState.Deleted
            }

            DeleteForMe -> {
                onlyForMe = true
                state = MessageState.Deleted
            }
        }
        pendingMessageStateDao.insert(PendingMessageStateEntity(message.id, channelId, state, null, onlyForMe))

        // Update message state in db and cache
        val deletedMessage = message.copy(state = state)
        onMessageEditedOrDeleted(deletedMessage)
        persistenceChannelsLogic.onMessageEditedOrDeleted(deletedMessage)

        return@withContext deleteMessageImpl(channelId, message.id, deleteType)
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

    private suspend fun deleteMessageImpl(channelId: Long, messageId: Long, deleteType: DeleteMessageType): SceytResponse<SceytMessage> {
        val response = messagesRepository.deleteMessage(channelId, messageId, deleteType)
        if (response is SceytResponse.Success) {
            response.data?.let { resultMessage ->
                pendingMessageStateDao.deleteByMessageId(resultMessage.id)
            }
        }
        return response
    }

    override suspend fun getMessageFromServerById(
            channelId: Long,
            messageId: Long,
    ): SceytResponse<SceytMessage> = withContext(dispatcherIO) {
        val result = messagesRepository.getMessageById(channelId, messageId)
        if (result is SceytResponse.Success) {
            result.data?.let { message ->
                messageDao.insertMessageIgnored(message.toMessageDb(true))
            }
        }
        return@withContext result
    }

    override suspend fun getMessageFromDbById(messageId: Long): SceytMessage? = withContext(dispatcherIO) {
        return@withContext messageDao.getMessageById(messageId)?.toSceytMessage()
    }

    override suspend fun getMessageFromDbByTid(tid: Long): SceytMessage? = withContext(dispatcherIO) {
        return@withContext messageDao.getMessageByTid(tid)?.toSceytMessage()
    }

    override suspend fun getMessagesFromDbByTid(tIds: List<Long>): List<SceytMessage> = withContext(dispatcherIO) {
        return@withContext messageDao.getMessagesByTid(tIds).map { it.toSceytMessage() }
    }

    override suspend fun attachmentSuccessfullySent(message: SceytMessage) = withContext(dispatcherIO) {
        messageDao.upsertMessage(message.toMessageDb(false))
        messagesCache.upsertNotifyUpdateAnyway(message.channelId, message)
    }

    override suspend fun saveChannelLastMessagesToDb(list: List<SceytMessage>?) {
        list ?: return
        withContext(dispatcherIO) {
            saveMessagesToDb(list)
            // Create ranges for last message
            list.forEach {
                messageLoadRangeUpdater.updateLoadRange(messageId = it.id, start = it.id, end = it.id, channelId = it.channelId)
            }
        }
    }

    override fun getOnMessageFlow() = onMessageFlow.asSharedFlow()

    override suspend fun sendTyping(channelId: Long, typing: Boolean) {
        messagesRepository.sendTyping(channelId, typing)
    }

    private fun loadMessages(
            loadType: LoadType, conversationId: Long, messageId: Long,
            replyInThread: Boolean, offset: Int, limit: Int,
            loadKey: LoadKeyData = LoadKeyData(value = messageId),
            ignoreDb: Boolean, ignoreServer: Boolean = false,
    ): Flow<PaginationResponse<SceytMessage>> {
        return callbackFlow {
            if (offset == 0) messagesCache.clear(conversationId)

            var dbResultWasEmpty = true
            // Load from database
            if (!ignoreDb) {
                trySend(getMessagesDbByLoadType(loadType, conversationId, messageId, offset, limit, loadKey).also {
                    dbResultWasEmpty = it.data.isEmpty()
                })
            }
            // Load from server
            if (!ignoreServer) {
                val response = getMessagesServerByLoadType(loadType, conversationId, messageId, offset,
                    limit, replyInThread, loadKey, ignoreDb, dbResultWasEmpty)

                trySend(response)
                updateMessageLoadRange(messageId, conversationId, response.data)

                // Mark messages as received
                markMessagesAsDelivered(conversationId, response.data.data ?: emptyList())
            }

            channel.close()
            awaitClose()
        }
    }

    private suspend fun updateMessageLoadRange(messageId: Long, channelId: Long, response: SceytResponse<List<SceytMessage>>) {
        val data = (response as? SceytResponse.Success)?.data ?: return
        if (data.isEmpty()) return
        messageLoadRangeUpdater.updateLoadRange(messageId = messageId, start = data.first().id, end = data.last().id, channelId = channelId)
    }

    private suspend fun updateMessageLoadRangeOnMessageEvent(
            message: SceytMessage,
            oldLastMessageId: Long?,
    ) {
        val channelId = message.channelId
        val oldLastMsgId = oldLastMessageId ?: channelCache.getOneOf(channelId)?.lastMessage?.id
        ?: persistenceChannelsLogic.getChannelFromDb(channelId)?.lastMessage?.id
        if (oldLastMsgId != null)
            messageLoadRangeUpdater.updateLoadRange(messageId = oldLastMsgId,
                start = oldLastMsgId, end = message.id, channelId = channelId)
    }

    private suspend fun markMessagesAsDelivered(channelId: Long, messages: List<SceytMessage>) {
        if (messages.isEmpty()) return
        // Mark messages as received
        withContext(Dispatchers.IO) {
            checkAndMarkChannelMessagesAsDelivered(channelId, messages)
        }
    }

    private suspend fun getMessagesDbByLoadType(
            loadType: LoadType, channelId: Long, lastMessageId: Long,
            offset: Int, limit: Int, loadKey: LoadKeyData,
    ): PaginationResponse.DBResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        val messages: List<MessageDb>

        clearOutdatedMessages(channelId)

        when (loadType) {
            LoadPrev -> {
                messages = getPrevMessagesDb(channelId, lastMessageId, offset, limit)
                hasPrev = messages.size == messagesLoadSize
            }

            LoadNext -> {
                messages = getNextMessagesDb(channelId, lastMessageId, offset, limit)
                hasNext = messages.size == messagesLoadSize
            }

            LoadNear -> {
                val data = getNearMessagesDb(channelId, lastMessageId, offset, limit)
                messages = data.data
                hasPrev = data.hasPrev
                hasNext = data.hasNext
            }

            LoadNewest -> {
                messages = getPrevMessagesDb(channelId, Long.MAX_VALUE, offset, limit)
                hasPrev = messages.size == messagesLoadSize
            }
        }

        val sceytMessages = messages.map { it.toSceytMessage() }
        messagesCache.addAll(channelId, sceytMessages, checkDifference = false, checkDiffAndNotifyUpdate = false)

        return PaginationResponse.DBResponse(sceytMessages, loadKey, offset, hasNext, hasPrev, loadType)
    }

    private suspend fun getMessagesServerByLoadType(
            loadType: LoadType, channelId: Long, lastMessageId: Long,
            offset: Int, limit: Int, replyInThread: Boolean,
            loadKey: LoadKeyData = LoadKeyData(value = lastMessageId),
            ignoreDb: Boolean, dbResultWasEmpty: Boolean,
    ): PaginationResponse.ServerResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        var hasDiff: Boolean
        var forceHasDiff = false
        var messages: List<SceytMessage> = emptyList()
        val response: SceytResponse<List<SceytMessage>>

        if (loadType != LoadNear)
            ConnectionEventManager.awaitToConnectSceyt()

        when (loadType) {
            LoadPrev -> {
                var msgId = lastMessageId
                if (offset == 0)
                    msgId = Long.MAX_VALUE

                response = messagesRepository.getPrevMessages(channelId, msgId, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == messagesLoadSize
                    if (offset == 0) {
                        persistenceChannelsLogic.updateLastMessageIfNeeded(channelId, messages.lastOrNull())
                    }
                    // Check maybe messages was cleared
                    if (offset == 0 && messages.isEmpty()) {
                        messageDao.deleteAllMessagesExceptPending(channelId)
                        rangeDao.deleteChannelLoadRanges(channelId)
                        messagesCache.clearAllExceptPending(channelId)
                        forceHasDiff = true
                    }
                }
            }

            LoadNext -> {
                response = messagesRepository.getNextMessages(channelId, lastMessageId, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasNext = response.data?.size == messagesLoadSize
                }
            }

            LoadNear -> {
                response = messagesRepository.getNearMessages(channelId, lastMessageId, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    val groupOldAndNewData = messages.groupBy { it.id > lastMessageId }

                    val newest = groupOldAndNewData[true]
                    val oldest = groupOldAndNewData[false]

                    hasNext = (newest?.size ?: 0) >= messagesLoadSize / 2
                    hasPrev = (oldest?.size ?: 0) >= messagesLoadSize / 2
                }
            }

            LoadNewest -> {
                response = messagesRepository.getPrevMessages(channelId, Long.MAX_VALUE, replyInThread, limit)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == messagesLoadSize
                }
            }
        }
        val updatedMessages = saveMessagesToDb(messages)
        hasDiff = messagesCache.addAll(channelId, updatedMessages, checkDifference = true, checkDiffAndNotifyUpdate = false)

        if (forceHasDiff) hasDiff = true

        return PaginationResponse.ServerResponse(
            data = response, cacheData = messagesCache.getSorted(channelId),
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb, dbResultWasEmpty = dbResultWasEmpty)
    }

    private fun findAndUpdateAttachmentPayLoads(message: SceytMessage, payloads: List<AttachmentPayLoadDb>): SceytMessage {
        payloads.filter { payLoad -> payLoad.payLoadEntity.messageTid == message.tid }.let { entity ->
            val attachments = message.attachments?.toMutableList() ?: return message
            attachments.forEachIndexed { index, attachment ->
                val predicate: (AttachmentPayLoadDb) -> Boolean = if (attachment.url.isNotNullOrBlank()) {
                    { entity.any { it.payLoadEntity.url == attachment.url } }
                } else {
                    { entity.any { it.payLoadEntity.filePath == attachment.filePath } }
                }

                payloads.find(predicate)?.let {
                    with(it.payLoadEntity) {
                        attachments[index] = attachment.copy(
                            transferState = transferState,
                            progressPercent = progressPercent,
                            filePath = filePath,
                            url = url,
                            linkPreviewDetails = it.linkPreviewDetails?.toLinkPreviewDetails(attachment.isHiddenLinkDetails())
                        )
                    }
                }
            }
            return message.copy(attachments = attachments)
        }
    }

    private suspend fun getPrevMessagesDb(channelId: Long, lastMessageId: Long, offset: Int, limit: Int): List<MessageDb> {
        var sentLastMessage = lastMessageId
        if (sentLastMessage == 0L) {
            sentLastMessage = messageDao.getLastSentMessageId(channelId) ?: 0
        }
        var messages = if (sentLastMessage == 0L) {
            emptyList()
        } else {
            (if (offset == 0)
                messageDao.getOldestThenMessagesInclude(channelId, sentLastMessage, limit)
            else messageDao.getOldestThenMessages(channelId, sentLastMessage, limit)).reversed()
        }
        if (offset == 0)
            messages = getPendingMessagesAndAddToList(channelId, messages)

        return messages
    }

    private suspend fun getNextMessagesDb(channelId: Long, lastMessageId: Long, offset: Int, limit: Int): List<MessageDb> {
        var messages = messageDao.getNewestThenMessage(channelId, lastMessageId, limit)

        if (offset == 0)
            messages = getPendingMessagesAndAddToList(channelId, messages)

        return messages
    }

    private suspend fun getNearMessagesDb(channelId: Long, messageId: Long, offset: Int, limit: Int): LoadNearData<MessageDb> {
        var data = messageDao.getNearMessages(channelId, messageId, limit)
        val messages = data.data

        if (offset == 0)
            data = data.copy(data = getPendingMessagesAndAddToList(channelId, messages))

        return data
    }

    private suspend fun getPendingMessagesAndAddToList(channelId: Long, list: List<MessageDb>): List<MessageDb> {
        val pendingMessage = messageDao.getPendingMessages(channelId)

        return if (pendingMessage.isNotEmpty()) {
            list.toMutableList().run {
                addAll(pendingMessage)
                sortedBy { it.messageEntity.createdAt }
            }
        } else list
    }

    private suspend fun clearOutdatedMessages(channelId: Long) {
        val outdatedMessages = autoDeleteMessageDao.getOutdatedMessages(
            channelId = channelId,
            localTime = System.currentTimeMillis()
        ).takeIf { it.isNotEmpty() } ?: return
        messageDao.deleteMessagesByTid(outdatedMessages.map { message -> message.messageTid })
        channelCache.messagesDeletedWithAutoDelete(channelId, outdatedMessages)
    }

    private suspend fun saveMessagesToDb(
            list: List<SceytMessage>?,
            includeParents: Boolean = true,
            unListAll: Boolean = false,
            replaceUserOnConflict: Boolean = true
    ): List<SceytMessage> {
        if (list.isNullOrEmpty()) return emptyList()
        val pendingStates = pendingMessageStateDao.getAll()
        val usersDb = mutableSetOf<UserDb>()
        val messagesDb = arrayListOf<MessageDb>()
        val parentMessagesDb = arrayListOf<MessageDb>()

        val mutableList = list.toArrayList()
        for ((index, message) in list.withIndex()) {
            updateMessageStatesWithPendingStates(message, pendingStates)?.let { updatedMessage ->
                mutableList[index] = updatedMessage
            }

            if (includeParents) {
                message.parentMessage?.let { parent ->
                    if (parent.id != 0L) {
                        parentMessagesDb.add(parent.toMessageDb(true))
                        if (parent.incoming && parent.user != null) {
                            usersDb.add(parent.user.toUserDb())
                        }
                    }
                }
            }

            messagesDb.add(message.toMessageDb(unListAll))
            if (message.incoming && message.user != null) {
                usersDb.add(message.user.toUserDb())
            }

            message.mentionedUsers?.let {
                usersDb.addAll(it.map { user -> user.toUserDb() })
            }
        }

        messageDao.upsertMessages(messagesDb)
        if (parentMessagesDb.isNotEmpty())
            messageDao.insertMessagesIgnored(parentMessagesDb)

        userDao.insertUsersWithMetadata(usersDb.toList(), replaceUserOnConflict)
        return mutableList
    }

    private fun updateMessageStatesWithPendingStates(message: SceytMessage, pendingStates: List<PendingMessageStateEntity>): SceytMessage? {
        return pendingStates.find { it.messageId == message.id }?.let {
            val body = if (it.state == MessageState.Edited && !it.editBody.isNullOrBlank())
                it.editBody else message.body
            message.copy(body = body, state = it.state)
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
            it.incoming && it.userMarkers?.any { marker -> marker.name == Received.value } != true
        }
        if (notDisplayedMessages.isNotEmpty())
            markMessagesAsImpl(channelId, Received, *notDisplayedMessages.map { it.id }.toLongArray())
    }

    private suspend fun markMessagesAsImpl(
            channelId: Long, marker: MarkerType,
            vararg ids: Long,
    ): List<SceytResponse<MessageListMarker>> = withContext(dispatcherIO) {
        val responseList = mutableListOf<SceytResponse<MessageListMarker>>()
        ids.toList().chunked(50).forEach {
            val typedArray = it.toLongArray()
            addPendingMarkerToDb(channelId, marker.value, *typedArray)

            val response = messagesRepository.markMessageAs(channelId, marker, *typedArray)

            onMarkerResponse(channelId, response, marker.value, *typedArray)
            responseList.add(response)
        }

        return@withContext responseList
    }

    private suspend fun addMessagesMarkerImpl(
            channelId: Long, marker: String,
            saveToPendingBeforeSend: Boolean,
            vararg ids: Long,
    ): List<SceytResponse<MessageListMarker>> = withContext(dispatcherIO) {
        val responseList = mutableListOf<SceytResponse<MessageListMarker>>()
        ids.toList().chunked(50).forEach {
            val typedArray = it.toLongArray()
            if (saveToPendingBeforeSend)
                addPendingMarkerToDb(channelId, marker, *typedArray)

            val response = messagesRepository.addMessagesMarker(channelId, marker, *typedArray)

            onMarkerResponse(channelId, response, marker, *typedArray)
            responseList.add(response)
        }

        return@withContext responseList
    }

    private suspend fun addPendingMarkerToDb(channelId: Long, marker: String, vararg ids: Long) {
        if (ids.isEmpty()) return
        val existMessageIds = messageDao.getExistMessageByIds(ids.toList())
        if (existMessageIds.isEmpty()) return
        val list = existMessageIds.map { PendingMarkerEntity(channelId = channelId, messageId = it, name = marker) }
        pendingMarkerDao.insertMany(list)
    }

    private suspend fun onMarkerResponse(
            channelId: Long,
            response: SceytResponse<MessageListMarker>,
            status: String,
            vararg ids: Long
    ) {
        when (response) {
            is SceytResponse.Success -> {
                response.data?.let { data ->
                    SceytLog.i("onMarkerResponse", "send $status, ${ids.toList()}, in response ${data.messageIds}")
                    val responseIds = data.messageIds.toList()

                    status.toDeliveryStatus()?.let { deliveryStatus ->
                        messageDao.updateMessagesStatus(channelId, responseIds, deliveryStatus)
                        val tIds = messageDao.getMessageTIdsByIds(*responseIds.toLongArray())
                        messagesCache.updateMessagesStatus(channelId, deliveryStatus, *tIds.toLongArray())
                    }

                    pendingMarkerDao.deleteMessagesMarkersByStatus(responseIds, status)
                    val existMessageIds = messageDao.getExistMessageByIds(responseIds)
                    myId?.let { userId ->
                        messageDao.insertUserMarkersAndLinks(existMessageIds.map {
                            MarkerEntity(messageId = it, userId = userId, name = data.name)
                        })
                    }
                }
            }

            is SceytResponse.Error -> {
                // Check if error code is 1301 (TypeNotAllowed), 1228 (TypeBadParam) then delete pending markers
                val code = response.exception?.code
                if (code == 1301 || code == 1228)
                    pendingMarkerDao.deleteMessagesMarkersByStatus(ids.toList(), status)
            }
        }
    }
}