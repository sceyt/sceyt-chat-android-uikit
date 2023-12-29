package com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic

import android.util.Size
import androidx.lifecycle.asFlow
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.LoadNearData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNear
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNewest
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadPrev
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentWithUserData
import com.sceyt.sceytchatuikit.data.models.messages.FileChecksumData
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.AttachmentsRepository
import com.sceyt.sceytchatuikit.data.toSceytAttachment
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.dao.AttachmentDao
import com.sceyt.sceytchatuikit.persistence.dao.FileChecksumDao
import com.sceyt.sceytchatuikit.persistence.dao.LinkDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageIdAndTid
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.AttachmentsCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.mappers.getTid
import com.sceyt.sceytchatuikit.persistence.mappers.toAttachment
import com.sceyt.sceytchatuikit.persistence.mappers.toFileChecksumData
import com.sceyt.sceytchatuikit.persistence.mappers.toLinkDetailsEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toLinkPreviewDetails
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toUser
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.utils.FileChecksumCalculator
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
        private val attachmentsRepository: AttachmentsRepository) : PersistenceAttachmentLogic, SceytKoinComponent {

    private val messagesLogic: PersistenceMessagesLogic by inject()

    override suspend fun setupFileTransferUpdateObserver() {
        FileTransferHelper.onTransferUpdatedLiveData.asFlow().collect {
            attachmentsCache.updateAttachmentTransferData(it)
        }
    }

    override suspend fun getAllPayLoadsByMsgTid(tid: Long): List<AttachmentPayLoadDb> {
        return attachmentDao.getAllAttachmentPayLoadsByMsgTid(tid)
    }

    override suspend fun getPrevAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>,
                                            offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadPrev, conversationId, lastAttachmentId, types, loadKeyData, offset, ignoreDb)
    }

    override suspend fun getNextAttachments(conversationId: Long, lastAttachmentId: Long, types: List<String>,
                                            offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadNext, conversationId, lastAttachmentId, types, loadKeyData, offset, ignoreDb)
    }

    override suspend fun getNearAttachments(conversationId: Long, attachmentId: Long, types: List<String>,
                                            offset: Int, ignoreDb: Boolean, loadKeyData: LoadKeyData): Flow<PaginationResponse<AttachmentWithUserData>> {
        return loadAttachments(loadType = LoadNear, conversationId, attachmentId, types, loadKeyData, offset, ignoreDb)
    }

    override suspend fun updateAttachmentIdAndMessageId(message: SceytMessage) {
        message.attachments?.forEach { attachment ->
            attachmentDao.updateAttachmentIdAndMessageId(attachment.id, message.id, message.tid, attachment.url)
        }
    }

    override suspend fun updateTransferDataByMsgTid(data: TransferData) {
        messagesCache.updateAttachmentTransferData(data)
        attachmentDao.updateAttachmentTransferDataByMsgTid(data.messageTid, data.progressPercent, data.state)
    }

    override suspend fun updateAttachmentWithTransferData(data: TransferData) {
        messagesCache.updateAttachmentTransferData(data)
        attachmentDao.updateAttachmentAndPayLoad(data)
    }

    override suspend fun updateAttachmentFilePathAndMetadata(messageTid: Long, newPath: String, fileSize: Long, metadata: String?) {
        messagesCache.updateAttachmentFilePathAndMeta(messageTid, newPath, metadata)
        attachmentDao.updateAttachmentFilePathAndMetadata(messageTid, newPath, fileSize, metadata)
    }

    override suspend fun getFileChecksumData(filePath: String?): FileChecksumData? {
        val checksum = FileChecksumCalculator.calculateFileChecksum(filePath ?: return null)
        return fileChecksumDao.getChecksum(checksum ?: return null)?.toFileChecksumData()
    }

    override suspend fun getLinkPreviewData(link: String?): SceytResponse<LinkPreviewDetails> = withContext(Dispatchers.IO) {
        if (link.isNullOrBlank()) return@withContext SceytResponse.Error(SceytException(0, "Link is null or blank: link -> $link"))

        linkDao.getLinkDetailsEntity(link)?.let {
            return@withContext SceytResponse.Success(it.toLinkPreviewDetails())
        }

        return@withContext when (val response = attachmentsRepository.getLinkPreviewData(link)) {
            is SceytResponse.Success -> {
                if (response.data != null) {
                    val details = response.data.toLinkPreviewDetails(link)
                    messagesCache.updateAttachmentLinkDetails(details)
                    attachmentsCache.updateAttachmentLinkDetails(details)
                    linkDao.insert(response.data.toLinkDetailsEntity(link, null))
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

    private fun loadAttachments(loadType: PaginationResponse.LoadType, conversationId: Long, attachmentId: Long,
                                types: List<String>, loadKey: LoadKeyData = LoadKeyData(value = attachmentId),
                                offset: Int, ignoreDb: Boolean): Flow<PaginationResponse<AttachmentWithUserData>> {
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

    private suspend fun getAttachmentsDbByLoadType(loadType: PaginationResponse.LoadType, conversationId: Long,
                                                   attachmentId: Long, types: List<String>, loadKey: LoadKeyData, offset: Int): PaginationResponse<AttachmentWithUserData> {

        var hasNext = false
        var hasPrev = false
        val attachments: List<SceytAttachment>

        when (loadType) {
            LoadPrev -> {
                attachments = getPrevAttachmentsDb(conversationId, attachmentId, types)
                hasPrev = attachments.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
            }

            LoadNext -> {
                attachments = getNextAttachmentsDb(conversationId, attachmentId, types)
                hasNext = attachments.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
            }

            LoadNear -> {
                val data = getNearAttachmentsDb(conversationId, attachmentId, types)
                attachments = data.data.map { it.toAttachment() }
                hasPrev = data.hasPrev
                hasNext = data.hasNext
            }

            LoadNewest -> {
                attachments = getPrevAttachmentsDb(conversationId, Long.MAX_VALUE, types)
                hasPrev = attachments.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
            }
        }

        val users = userDao.getUsersById(attachments.mapNotNull { it.userId })

        val data = arrayListOf<AttachmentWithUserData>()

        attachments.map {
            data.add(AttachmentWithUserData(it, users.find { userEntity -> userEntity.id == it.userId }?.toUser()))
        }
        attachmentsCache.addAll(attachments, false)

        return PaginationResponse.DBResponse(data, loadKey, offset, hasNext, hasPrev, loadType)
    }

    private suspend fun getPrevAttachmentsDb(channelId: Long, lastAttachmentId: Long, types: List<String>): List<SceytAttachment> {
        val id = if (lastAttachmentId == 0L) Long.MAX_VALUE else lastAttachmentId
        val attachments = attachmentDao.getOldestThenAttachment(channelId, id, SceytKitConfig.ATTACHMENTS_LOAD_SIZE, types)
        return attachments.map { attachmentDb -> attachmentDb.toAttachment() }.reversed()
    }

    private suspend fun getNextAttachmentsDb(channelId: Long, lastAttachmentId: Long, types: List<String>): List<SceytAttachment> {
        val attachments = attachmentDao.getNewestThenAttachment(channelId, lastAttachmentId, SceytKitConfig.ATTACHMENTS_LOAD_SIZE, types)
        return attachments.map { attachmentDb -> attachmentDb.toAttachment() }
    }

    private suspend fun getNearAttachmentsDb(channelId: Long, attachmentId: Long, types: List<String>): LoadNearData<AttachmentDb> {
        return attachmentDao.getNearAttachments(channelId, attachmentId, SceytKitConfig.ATTACHMENTS_LOAD_SIZE, types)
    }

    private suspend fun getAttachmentsServerByLoadType(loadType: PaginationResponse.LoadType, conversationId: Long,
                                                       attachmentId: Long, types: List<String>,
                                                       loadKey: LoadKeyData, offset: Int, ignoreDb: Boolean): PaginationResponse<AttachmentWithUserData> {
        var hasNext = false
        var hasPrev = false
        var hasDiff = false
        val response: SceytResponse<Pair<List<Attachment>, Map<String, User>>>

        when (loadType) {
            LoadPrev -> {
                response = attachmentsRepository.getPrevAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success)
                    hasPrev = response.data?.first?.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
            }

            LoadNext -> {
                response = attachmentsRepository.getNextAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success)
                    hasPrev = response.data?.first?.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
            }

            LoadNear -> {
                response = attachmentsRepository.getNearAttachments(conversationId, attachmentId, types)
                if (response is SceytResponse.Success) {
                    response.data?.let { pair ->
                        val groupOldAndNewData = pair.first.groupBy { it.id > attachmentId }
                        val newest = groupOldAndNewData[true]
                        val oldest = groupOldAndNewData[false]

                        hasNext = (newest?.size ?: 0) >= SceytKitConfig.MESSAGES_LOAD_SIZE / 2
                        hasPrev = (oldest?.size ?: 0) >= SceytKitConfig.MESSAGES_LOAD_SIZE / 2
                    }
                }
            }

            LoadNewest -> {
                response = attachmentsRepository.getPrevAttachments(conversationId, Long.MAX_VALUE, types)
                if (response is SceytResponse.Success)
                    hasPrev = response.data?.first?.size == SceytKitConfig.ATTACHMENTS_LOAD_SIZE
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

    private suspend fun handelServerResponse(conversationId: Long,
                                             response: SceytResponse<Pair<List<Attachment>, Map<String, User>>>): SceytResponse<List<AttachmentWithUserData>> {
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
                    transferState = this.transferState ?: TransferState.PendingDownload
                    progress = this.progressPercent ?: 0f
                    filePath = this.filePath
                }
                linkPreviewDetails = it.linkPreviewDetails?.toLinkPreviewDetails()
            }

            sceytAttachments.add(attachment.toSceytAttachment(messageTid, transferState, progress, linkPreviewDetails).apply {
                this.filePath = filePath
            })
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