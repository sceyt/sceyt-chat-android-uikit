package com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic

import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.*
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.data.repositories.AttachmentsRepository
import com.sceyt.sceytchatuikit.data.toSceytAttachment
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCash
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PersistenceAttachmentLogicImpl(
        private val messageDao: MessageDao,
        private val messagesCash: MessagesCash,
        private val attachmentsRepository: AttachmentsRepository) : PersistenceAttachmentLogic {

    override suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadEntity> {
        return messageDao.getAllPayLoadsByMsgTid(tid)
    }

    override suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadPrev, conversationId, lastAttachmentId, types, offset = 0, loadKey = LoadKeyData(), false)
    }

    override suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadNext, conversationId, lastAttachmentId, types, offset = 0, loadKey = LoadKeyData(), false)
    }

    override suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadNear, conversationId, attachmentId, types, offset = 0, loadKey = LoadKeyData(), false)
    }

    override fun updateTransferDataByMsgTid(data: TransferData) {
        messageDao.updateAttachmentTransferDataByMsgTid(data.messageTid, data.progressPercent, data.state)
        messagesCash.updateAttachmentTransferData(data)
    }

    override fun updateAttachmentWithTransferData(data: TransferData) {
        messageDao.updateAttachmentAndPayLoad(data)
        messagesCash.updateAttachmentTransferData(data)
    }

    override fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?) {
        messageDao.updateAttachmentFilePathAndMetadata(messageTid, newPath, fileSize, metadata)
        messagesCash.updateAttachmentFilePathAndMeta(messageTid, newPath, metadata)
    }

    private fun loadAttachments(loadType: PaginationResponse.LoadType, conversationId: Long, attachmentId: Long,
                                types: List<String>, offset: Int, loadKey: LoadKeyData = LoadKeyData(value = attachmentId),
                                ignoreDb: Boolean): Flow<PaginationResponse<AttachmentWithUserData>> {
        return callbackFlow {
            if (offset == 0) messagesCash.clear()

            // Load from database
            if (!ignoreDb)
                trySend(getAttachmentsDbByLoadType(loadType, conversationId, attachmentId, types, offset, loadKey))
            // Load from server
            trySend(getAttachmentsServerByLoadType(loadType, conversationId, attachmentId, types, offset,
                loadKey, ignoreDb))

            channel.close()
            awaitClose()
        }
    }

    private suspend fun getAttachmentsDbByLoadType(loadType: PaginationResponse.LoadType, conversationId: Long,
                                                   attachmentId: Long, types: List<String>, offset: Int, loadKey: LoadKeyData): PaginationResponse<AttachmentWithUserData> {

        return PaginationResponse.DBResponse(emptyList(), loadKey, offset, loadType = loadType)
    }

    private suspend fun getAttachmentsServerByLoadType(loadType: PaginationResponse.LoadType, conversationId: Long,
                                                       attachmentId: Long, types: List<String>, offset: Int, loadKey: LoadKeyData, ignoreDb: Boolean): PaginationResponse<AttachmentWithUserData> {
        var hasNext = false
        var hasPrev = false
        val hasDiff = true
        var data: List<AttachmentWithUserData>
        var mappedResponse: SceytResponse<List<AttachmentWithUserData>> = SceytResponse.Error()

        when (loadType) {
            LoadPrev -> {
                val response = attachmentsRepository.getPrevAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success) {
                    response.data?.let { pair ->
                        data = pair.first.map {
                            AttachmentWithUserData(it.toSceytAttachment(it.messageId,
                                TransferState.PendingDownload, 0f), pair.second[it.userId])
                        }
                        mappedResponse = SceytResponse.Success(data)
                    }
                    hasPrev = response.data?.first?.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
                } else
                    mappedResponse = SceytResponse.Error((response as SceytResponse.Error).exception)
            }
            LoadNext -> {
                val response = attachmentsRepository.getNextAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success) {
                    response.data?.let { pair ->
                        data = pair.first.map {
                            AttachmentWithUserData(it.toSceytAttachment(it.messageId,
                                TransferState.PendingDownload, 0f), pair.second[it.userId])
                        }
                        mappedResponse = SceytResponse.Success(data)
                    }
                    hasPrev = response.data?.first?.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
                } else
                    mappedResponse = SceytResponse.Error((response as SceytResponse.Error).exception)
            }
            LoadNear -> {
                val response = attachmentsRepository.getNearAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success) {
                    response.data?.let { pair ->
                        data = pair.first.map {
                            AttachmentWithUserData(it.toSceytAttachment(it.messageId,
                                TransferState.PendingDownload, 0f), pair.second[it.userId])
                        }
                        mappedResponse = SceytResponse.Success(data)

                        val groupOldAndNewData = pair.first.groupBy { it.id > attachmentId }
                        val newest = groupOldAndNewData[true]
                        val oldest = groupOldAndNewData[false]

                        hasNext = (newest?.size ?: 0) >= SceytKitConfig.MESSAGES_LOAD_SIZE / 2
                        hasPrev = (oldest?.size ?: 0) >= SceytKitConfig.MESSAGES_LOAD_SIZE / 2
                    }
                } else
                    mappedResponse = SceytResponse.Error((response as SceytResponse.Error).exception)
            }
            else -> throw java.lang.Exception("Load type $loadType not supported from attachments")

            // saveMessagesToDb(messages)
            // val tIds = getMessagesTid(messages)
            // val payloads = messageDao.getAllPayLoadsByMsgTid(*tIds.toLongArray())

            /*  messages.forEach {
                  findAndUpdateAttachmentPayLoads(it, payloads)
                  it.parent?.let { parent -> findAndUpdateAttachmentPayLoads(parent, payloads) }
              }*/

            /* if (loadType == LoadNear && loadKey.key == LoadKeyType.ScrollToMessageById.longValue)
                 messagesCash.clear()*/

            //hasDiff = messagesCash.addAll(messages, true)
        }
        return PaginationResponse.ServerResponse(
            data = mappedResponse, cashData = emptyList(),
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb)
    }


    /* private suspend fun getMessagesServerByLoadType(loadType: LoadType, channelId: Long, lastMessageId: Long,
                                                    offset: Int, replyInThread: Boolean, loadKey: LoadKeyData = LoadKeyData(value = lastMessageId),
                                                    ignoreDb: Boolean): PaginationResponse.ServerResponse<SceytMessage> {
        var hasNext = false
        var hasPrev = false
        val hasDiff: Boolean
        var messages: List<SceytMessage> = emptyList()
        val response: SceytResponse<List<SceytMessage>>

        when (loadType) {
            LoadPrev -> {
                response = messagesRepository.getPrevMessages(channelId, lastMessageId, replyInThread)
                if (response is SceytResponse.Success) {
                    messages = response.data ?: arrayListOf()
                    hasPrev = response.data?.size == MESSAGES_LOAD_SIZE
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
        val payloads = messageDao.getAllPayLoadsByMsgTid(*tIds.toLongArray())

        messages.forEach {
            findAndUpdateAttachmentPayLoads(it, payloads)
            it.parent?.let { parent -> findAndUpdateAttachmentPayLoads(parent, payloads) }
        }

        if (loadType == LoadNear && loadKey.key == LoadKeyType.ScrollToMessageById.longValue)
            messagesCash.clear()

        hasDiff = messagesCash.addAll(messages, true)

        // Mark messages as received
        markChannelMessagesAsDelivered(channelId, messages)

        return PaginationResponse.ServerResponse(
            data = response, cashData = messagesCash.getSorted(),
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb)
    }*/
}