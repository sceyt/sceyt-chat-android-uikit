package com.sceyt.chatuikit.persistence.logicimpl.attachment

import android.util.Size
import androidx.lifecycle.asFlow
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.chatuikit.data.models.messages.FileChecksumData
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.database.dao.LinkDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageIdAndTid
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.getTid
import com.sceyt.chatuikit.persistence.mappers.isHiddenLinkDetails
import com.sceyt.chatuikit.persistence.mappers.toAttachment
import com.sceyt.chatuikit.persistence.mappers.toFileChecksumData
import com.sceyt.chatuikit.persistence.mappers.toLinkDetailsEntity
import com.sceyt.chatuikit.persistence.mappers.toLinkPreviewDetails
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytAttachment
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.repositories.AttachmentsRepository
import com.sceyt.chatuikit.shared.utils.FileChecksumCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

internal class PersistenceAttachmentLogicImpl(
        private val messageDao: MessageDao,
        private val attachmentDao: AttachmentDao,
        private val userDao: UserDao,
        private val fileChecksumDao: FileChecksumDao,
        private val linkDao: LinkDao,
        private val messagesCache: MessagesCache,
        private val attachmentsCache: AttachmentsCache,
        private val attachmentsRepository: AttachmentsRepository,
) : PersistenceAttachmentLogic, SceytKoinComponent {

    private val messagesLogic: PersistenceMessagesLogic by inject()
    private val attachmentsLoadSize get() = SceytChatUIKit.config.queryLimits.attachmentListQueryLimit

    override suspend fun setupFileTransferUpdateObserver() {
        FileTransferHelper.onTransferUpdatedLiveData.asFlow().collect {
            attachmentsCache.updateAttachmentTransferData(it)
        }
    }

    override suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadDb> {
        return attachmentDao.getAllAttachmentPayLoadsByMsgTid(tid)
    }

    override suspend fun getPrevAttachments(
            conversationId: Long, lastAttachmentId: Long, types: List<String>,
            offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadPrev, conversationId, lastAttachmentId, types, loadKeyData, offset, ignoreDb)
    }

    override suspend fun getNextAttachments(
            conversationId: Long, lastAttachmentId: Long, types: List<String>,
            offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadNext, conversationId, lastAttachmentId, types, loadKeyData, offset, ignoreDb)
    }

    override suspend fun getNearAttachments(
            conversationId: Long, attachmentId: Long, types: List<String>,
            offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadNear, conversationId, attachmentId, types, loadKeyData, offset, ignoreDb)
    }

    override suspend fun updateAttachmentIdAndMessageId(message: SceytMessage) = withContext(Dispatchers.IO) {
        message.attachments?.forEach { attachment ->
            attachmentDao.updateAttachmentIdAndMessageId(attachment.id, message.id, message.tid, attachment.url)
        }
        Unit
    }

    override suspend fun updateTransferDataByMsgTid(data: TransferData) {
        messagesCache.updateAttachmentTransferData(data)
        withContext(Dispatchers.IO) {
            attachmentDao.updateAttachmentTransferDataByMsgTid(data.messageTid, data.progressPercent, data.state)
        }
    }

    override suspend fun updateAttachmentWithTransferData(data: TransferData) {
        messagesCache.updateAttachmentTransferData(data)
        withContext(Dispatchers.IO) {
            attachmentDao.updateAttachmentAndPayLoad(data)
        }
    }

    override suspend fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?) {
        messagesCache.updateAttachmentFilePathAndMeta(messageTid, newPath, metadata)
        withContext(Dispatchers.IO) {
            attachmentDao.updateAttachmentFilePathAndMetadata(messageTid, newPath, fileSize, metadata)
        }
    }

    override suspend fun getFileChecksumData(filePath: String?): FileChecksumData? {
        val checksum = FileChecksumCalculator.calculateFileChecksum(filePath ?: return null)
        return fileChecksumDao.getChecksum(checksum ?: return null)?.toFileChecksumData()
    }

    override suspend fun getLinkPreviewData(link: String?): SceytResponse<LinkPreviewDetails> = withContext(Dispatchers.IO) {
        if (link.isNullOrBlank()) return@withContext SceytResponse.Error(SceytException(0, "Link is null or blank: link -> $link"))

        linkDao.getLinkDetailsEntity(link)?.let {
            return@withContext SceytResponse.Success(it.toLinkPreviewDetails(false))
        }

        return@withContext when (val response = attachmentsRepository.getLinkPreviewData(link)) {
            is SceytResponse.Success -> {
                if (response.data != null) {
                    val details = response.data.toLinkPreviewDetails(link)
                    messagesCache.updateAttachmentLinkDetails(details)
                    attachmentsCache.updateAttachmentLinkDetails(details)
                    linkDao.insert(details.toLinkDetailsEntity(link, null))
                    SceytResponse.Success(details)
                } else
                    SceytResponse.Error(SceytException(0, "Link is null or blank: link -> $link"))
            }

            is SceytResponse.Error -> SceytResponse.Error(response.exception, null)
        }
    }

    override suspend fun upsertLinkPreviewData(linkDetails: LinkPreviewDetails) = withContext(Dispatchers.IO) {
        linkDao.upsert(linkDetails.toLinkDetailsEntity())
        messagesCache.updateAttachmentLinkDetails(linkDetails)
        attachmentsCache.updateAttachmentLinkDetails(linkDetails)
    }

    override suspend fun updateLinkDetailsSize(link: String, size: Size) = withContext(Dispatchers.IO) {
        linkDao.updateSizes(link, size.width, size.height)
        messagesCache.updateLinkDetailsSize(link, size.width, size.height)
        attachmentsCache.updateLinkDetailsSize(link, size.width, size.height)
    }

    override suspend fun updateLinkDetailsThumb(link: String, thumb: String) = withContext(Dispatchers.IO) {
        linkDao.updateThumb(link, thumb)
        messagesCache.updateThumb(link, thumb)
        attachmentsCache.updateThumb(link, thumb)
    }

    override fun onTransferProgressPercentUpdated(transferData: TransferData) {
        messagesCache.updateAttachmentTransferData(transferData)
    }

    private fun loadAttachments(
            loadType: PaginationResponse.LoadType, conversationId: Long, attachmentId: Long,
            types: List<String>, loadKey: LoadKeyData = LoadKeyData(value = attachmentId),
            offset: Int, ignoreDb: Boolean,
    ): Flow<PaginationResponse<AttachmentWithUserData>> {
        return callbackFlow {
            if (offset == 0) attachmentsCache.clear(types)

            // Load from database
            if (!ignoreDb)
                trySend(getAttachmentsDbByLoadType(loadType, conversationId, attachmentId, types, loadKey, offset))
            // Load from server
            trySend(getAttachmentsServerByLoadType(loadType, conversationId, attachmentId, types,
                loadKey, offset, ignoreDb))

            channel.close()
            awaitClose()
        }
    }

    private suspend fun getAttachmentsDbByLoadType(
            loadType: PaginationResponse.LoadType, conversationId: Long,
            attachmentId: Long, types: List<String>, loadKey: LoadKeyData, offset: Int,
    ): PaginationResponse<AttachmentWithUserData> {

        var hasNext = false
        var hasPrev = false
        val attachments: List<SceytAttachment>

        when (loadType) {
            LoadPrev -> {
                attachments = getPrevAttachmentsDb(conversationId, attachmentId, types)
                hasPrev = attachments.size == attachmentsLoadSize
            }

            LoadNext -> {
                attachments = getNextAttachmentsDb(conversationId, attachmentId, types)
                hasNext = attachments.size == attachmentsLoadSize
            }

            LoadNear -> {
                val data = getNearAttachmentsDb(conversationId, attachmentId, types)
                attachments = data.data.map { it.toAttachment() }
                hasPrev = data.hasPrev
                hasNext = data.hasNext
            }

            LoadNewest -> {
                attachments = getPrevAttachmentsDb(conversationId, Long.MAX_VALUE, types)
                hasPrev = attachments.size == attachmentsLoadSize
            }
        }

        val users = userDao.getUsersById(attachments.mapNotNull { it.userId })

        val data = arrayListOf<AttachmentWithUserData>()

        attachments.map {
            data.add(AttachmentWithUserData(it, users.find { userEntity -> userEntity.id == it.userId }?.toSceytUser()))
        }
        attachmentsCache.addAll(attachments, false)

        return PaginationResponse.DBResponse(data, loadKey, offset, hasNext, hasPrev, loadType)
    }

    private suspend fun getPrevAttachmentsDb(channelId: Long, lastAttachmentId: Long, types: List<String>): List<SceytAttachment> {
        val id = if (lastAttachmentId == 0L) Long.MAX_VALUE else lastAttachmentId
        val attachments = attachmentDao.getOldestThenAttachment(channelId, id, attachmentsLoadSize, types)
        return attachments.map { attachmentDb -> attachmentDb.toAttachment() }.reversed()
    }

    private suspend fun getNextAttachmentsDb(channelId: Long, lastAttachmentId: Long, types: List<String>): List<SceytAttachment> {
        val attachments = attachmentDao.getNewestThenAttachment(channelId, lastAttachmentId, attachmentsLoadSize, types)
        return attachments.map { attachmentDb -> attachmentDb.toAttachment() }
    }

    private suspend fun getNearAttachmentsDb(channelId: Long, attachmentId: Long, types: List<String>): LoadNearData<AttachmentDb> {
        return attachmentDao.getNearAttachments(channelId, attachmentId, attachmentsLoadSize, types)
    }

    private suspend fun getAttachmentsServerByLoadType(
            loadType: PaginationResponse.LoadType, conversationId: Long,
            attachmentId: Long, types: List<String>,
            loadKey: LoadKeyData, offset: Int, ignoreDb: Boolean,
    ): PaginationResponse<AttachmentWithUserData> {
        var hasNext = false
        var hasPrev = false
        var hasDiff = false
        val response: SceytResponse<Pair<List<Attachment>, Map<String, SceytUser>>>

        when (loadType) {
            LoadPrev -> {
                response = attachmentsRepository.getPrevAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success)
                    hasPrev = response.data?.first?.size == attachmentsLoadSize
            }

            LoadNext -> {
                response = attachmentsRepository.getNextAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success)
                    hasPrev = response.data?.first?.size == attachmentsLoadSize
            }

            LoadNear -> {
                response = attachmentsRepository.getNearAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success) {
                    response.data?.let { pair ->
                        val groupOldAndNewData = pair.first.groupBy { it.id > attachmentId }
                        val newest = groupOldAndNewData[true]
                        val oldest = groupOldAndNewData[false]

                        hasNext = (newest?.size ?: 0) >= attachmentsLoadSize / 2
                        hasPrev = (oldest?.size ?: 0) >= attachmentsLoadSize / 2
                    }
                }
            }

            LoadNewest -> {
                response = attachmentsRepository.getPrevAttachments(conversationId, Long.MAX_VALUE, types)
                if (response is SceytResponse.Success)
                    hasPrev = response.data?.first?.size == attachmentsLoadSize
            }
        }

        val mappedResponse = handelServerResponse(conversationId, response)

        if (mappedResponse is SceytResponse.Success)
            mappedResponse.data?.let {
                hasDiff = attachmentsCache.addAll(it.map { data -> data.attachment }, true)
            }

        val cacheData = attachmentsCache.getSorted(types).map {
            AttachmentWithUserData(it, response.data?.second?.get(it.userId))
        }.reversed()

        return PaginationResponse.ServerResponse(
            data = mappedResponse, cacheData = cacheData,
            loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext,
            hasPrev = hasPrev, loadType = loadType, ignoredDb = ignoreDb)
    }

    private suspend fun handelServerResponse(
            conversationId: Long,
            response: SceytResponse<Pair<List<Attachment>, Map<String, SceytUser>>>,
    ): SceytResponse<List<AttachmentWithUserData>> {
        when (response) {
            is SceytResponse.Success -> {
                if (response.data?.first.isNullOrEmpty()) return SceytResponse.Success(emptyList())
                val attachments = response.data!!.first
                val usersMap = response.data.second

                // Checking maybe all messages is exist in database
                val existMsgIdsData = messageDao.getExistMessagesIdTidByIds(attachments.map { it.messageId })
                val attachmentsAndMissingMessages = getExistAttachmentsAndMissedMsgIds(attachments, existMsgIdsData)
                val sceytAttachments = attachmentsAndMissingMessages.first.toMutableList()

                // Load missed messages, and update attachments transfer state
                if (attachmentsAndMissingMessages.second.isNotEmpty())
                    when (val result = getAndCorrectAttachmentsData(conversationId, attachmentsAndMissingMessages.second, attachments)) {
                        is SceytResponse.Success -> sceytAttachments.addAll(result.data ?: listOf())
                        is SceytResponse.Error -> return SceytResponse.Error(result.exception)
                    }

                val finalData = sceytAttachments.map { AttachmentWithUserData(it, usersMap[it.userId]) }
                return SceytResponse.Success(finalData)
            }

            is SceytResponse.Error -> return SceytResponse.Error(response.exception)
        }
    }

    private suspend fun getExistAttachmentsAndMissedMsgIds(attachments: List<Attachment>, idsData: List<MessageIdAndTid>): Pair<List<SceytAttachment>, List<Long>> {
        if (attachments.isEmpty())
            return Pair(emptyList(), emptyList())

        val msgIds = attachments.map { it.messageId }
        val transferData = attachmentDao.getAllAttachmentPayLoadsByMsgTid(*idsData.map { it.tid }.toLongArray())
        val missedMsgIds: MutableList<Long> = msgIds.minus(idsData.mapNotNull { it.id }.toSet()).toMutableList()
        val sceytAttachments = arrayListOf<SceytAttachment>()

        for (attachment in attachments) {
            val messageTid = idsData.find { it.id == attachment.messageId }?.tid
            if (messageTid == null) {
                SceytLog.e(TAG, "Couldn't find message tid for msgId -> ${attachment.messageId}, added to missed messages list.")
                missedMsgIds.add(attachment.messageId)
                continue
            }

            var transferState = TransferState.PendingDownload
            var progress = 0f
            var filePath: String? = null
            var linkPreviewDetails: LinkPreviewDetails? = null

            transferData.find { it.payLoadEntity.messageTid == messageTid }?.let {
                with(it.payLoadEntity) {
                    transferState = this.transferState
                    progress = this.progressPercent ?: 0f
                    filePath = this.filePath
                }
                linkPreviewDetails = it.linkPreviewDetails?.toLinkPreviewDetails(attachment.isHiddenLinkDetails())
            }

            sceytAttachments.add(attachment.toSceytAttachment(messageTid, transferState, progress, linkPreviewDetails).copy(
                filePath = filePath
            ))
        }
        return Pair(sceytAttachments, missedMsgIds)
    }

    private suspend fun getAndCorrectAttachmentsData(conversationId: Long, messageIds: List<Long>, attachments: List<Attachment>): SceytResponse<List<SceytAttachment>> {
        return if (messageIds.isNotEmpty()) {
            val sceytAttachments = arrayListOf<SceytAttachment>()
            when (val messagesResponse = messagesLogic.loadMessagesById(conversationId, messageIds)) {
                is SceytResponse.Success -> {
                    messagesResponse.data?.let { data ->
                        messageDao.upsertMessages(data.map { it.toMessageDb(false) })
                        data.forEach {
                            attachments.find { attachment -> attachment.messageId == it.id }?.let { attachment ->
                                sceytAttachments.add(attachment.toSceytAttachment(
                                    messageTid = getTid(it.id, it.tid, it.incoming),
                                    transferState = TransferState.PendingDownload))
                            }
                        }
                    }
                    SceytResponse.Success(sceytAttachments)
                }

                is SceytResponse.Error -> SceytResponse.Error(messagesResponse.exception)
            }
        } else SceytResponse.Success(emptyList())
    }
}