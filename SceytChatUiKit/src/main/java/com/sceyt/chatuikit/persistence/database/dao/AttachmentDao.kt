package com.sceyt.chatuikit.persistence.database.dao

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_PAYLOAD_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadDb
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import kotlin.math.max

@Dao
internal abstract class AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAttachment(attachment: AttachmentEntity)

    @Transaction
    open suspend fun replaceAttachmentByType(attachment: AttachmentEntity) {
        deleteAttachmentsByMsgTidAndType(attachment.messageTid, attachment.type)
        insertAttachment(attachment)
    }

    @Transaction
    @Query(
        """
        SELECT *
        FROM $ATTACHMENT_TABLE
        WHERE channelId = :channelId
          AND id != 0
          AND id < :attachmentId
          AND type IN (:types)
          AND viewOnce != 1
        ORDER BY createdAt DESC, id DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getOldestThenAttachment(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $ATTACHMENT_TABLE
        WHERE channelId = :channelId
          AND id != 0
          AND id <= :attachmentId
          AND type IN (:types)
          AND viewOnce != 1
        ORDER BY createdAt DESC, id DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getOldestThenAttachmentInclude(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $ATTACHMENT_TABLE
        WHERE channelId = :channelId
          AND id != 0
          AND id > :attachmentId
          AND type IN (:types)
          AND viewOnce != 1
        ORDER BY createdAt, id
        LIMIT :limit
        """
    )
    abstract suspend fun getNewestThenAttachment(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $ATTACHMENT_TABLE
        WHERE channelId = :channelId
          AND id >= :attachmentId
          AND type IN (:types)
          AND viewOnce != 1
        ORDER BY createdAt, id
        LIMIT :limit
        """
    )
    abstract suspend fun getNewestThenAttachmentInclude(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    open suspend fun getNearAttachments(
        channelId: Long,
        attachmentId: Long,
        limit: Int,
        types: List<String>
    ): LoadNearData<AttachmentDb> {
        val oldest = getOldestThenAttachmentInclude(channelId, attachmentId, limit, types).reversed()
        val includesInOldest = oldest.lastOrNull()?.attachmentEntity?.id == attachmentId

        // If the message not exist then return empty list
        if (!includesInOldest)
            return LoadNearData(emptyList(), hasNext = false, hasPrev = false)

        val newest = getNewestThenAttachment(channelId, attachmentId, limit, types)
        val halfLimit = limit / 2

        val newestDiff = max(halfLimit - newest.size, 0)
        val oldestDiff = max((limit.toDouble() / 2).roundUp() - oldest.size, 0)

        var newAttachments = newest.take(halfLimit + oldestDiff)
        val oldAttachments = oldest.takeLast(halfLimit + newestDiff)

        if (oldAttachments.size < limit && newAttachments.size > halfLimit)
            newAttachments = newest.take(limit - oldAttachments.size)

        val hasPrev = oldest.size > halfLimit
        val hasNext = newest.size > halfLimit

        val data = (oldAttachments + newAttachments).sortedBy { it.attachmentEntity.createdAt }

        return LoadNearData(data, hasNext = hasNext, hasPrev = hasPrev)
    }

    @Transaction
    @Query("SELECT * FROM $ATTACHMENT_PAYLOAD_TABLE WHERE messageTid IN (:tid)")
    abstract suspend fun getAllAttachmentPayLoadsByMsgTid(vararg tid: Long): List<AttachmentPayLoadDb>

    @Query("SELECT * FROM $ATTACHMENT_TABLE WHERE messageTid = :msgTid AND type = :type LIMIT 1")
    abstract suspend fun getAttachmentByMsgTidAndType(msgTid: Long, type: String): AttachmentEntity?

    @Query("SELECT * FROM $ATTACHMENT_TABLE WHERE messageTid = :msgTid AND url = :url LIMIT 1")
    abstract suspend fun getAttachmentByMsgTidAndUrl(msgTid: Long, url: String): AttachmentEntity?

    @Query(
        """
        UPDATE $ATTACHMENT_TABLE
        SET id = :attachmentId, messageId = :messageId
        WHERE messageTid = :messageTid
          AND url = :attachmentUrl
        """
    )
    abstract suspend fun updateAttachmentIdAndMessageId(attachmentId: Long?, messageId: Long, messageTid: Long, attachmentUrl: String?)

    @Query("UPDATE $ATTACHMENT_PAYLOAD_TABLE SET progressPercent = :progress, transferState = :state WHERE messageTid = :tid")
    abstract suspend fun updateAttachmentTransferDataByMsgTid(tid: Long, progress: Float, state: TransferState)

    @Query(
        """
        UPDATE $ATTACHMENT_PAYLOAD_TABLE
        SET transferState = :waiting
        WHERE messageTid = :tid AND transferState = :pending
        """
    )
    abstract suspend fun markUploadQueued(
        tid: Long,
        waiting: TransferState = TransferState.WaitingToUpload,
        pending: TransferState = TransferState.PendingUpload,
    )

    @Transaction
    open suspend fun updateAttachmentAndPayLoad(transferData: TransferData) {
        updateAttachmentByMsgTid(transferData.messageTid, transferData.filePath, transferData.url)
        updateAttachmentPayLoadByMsgTid(transferData.messageTid, transferData.filePath, transferData.url,
            transferData.progressPercent, transferData.state)
    }

    @Transaction
    open suspend fun updateAttachmentFilePathAndMetadata(tid: Long, filePath: String?, fileSize: Long, metadata: String?) {
        updateAttachmentFilePathByMsgTid(tid, filePath, fileSize, metadata)
        updateAttachmentPayLoadFilePathByMsgTid(tid, filePath)
    }

    @Transaction
    open suspend fun updateUploadedAttachment(data: TransferData, fileSize: Long, metadata: String?) {
        updateAttachmentFilePathByMsgTid(data.messageTid, data.filePath, fileSize, metadata)
        updateAttachmentAndPayLoad(data)
    }

    @Query("UPDATE $ATTACHMENT_TABLE SET filePath = :filePath, url = :url WHERE messageTid = :msgTid AND type NOT IN (:ignoreTypes)")
    abstract suspend fun updateAttachmentByMsgTid(
        msgTid: Long,
        filePath: String?,
        url: String?,
        ignoreTypes: List<String> = untrackedAttachmentTypes
    )

    @Query(
        """
        UPDATE $ATTACHMENT_PAYLOAD_TABLE
        SET filePath = :filePath, url = :url, progressPercent = :progress, transferState = :state
        WHERE messageTid = :tid
        """
    )
    abstract suspend fun updateAttachmentPayLoadByMsgTid(tid: Long, filePath: String?, url: String?, progress: Float, state: TransferState)

    @Query(
        """
        UPDATE $ATTACHMENT_TABLE
        SET filePath = :filePath, fileSize = :fileSize, metadata = :metadata
        WHERE messageTid = :msgTid
          AND type NOT IN (:ignoreTypes)
        """
    )
    abstract suspend fun updateAttachmentFilePathByMsgTid(
        msgTid: Long,
        filePath: String?,
        fileSize: Long,
        metadata: String?,
        ignoreTypes: List<String> = untrackedAttachmentTypes
    )

    @Query("UPDATE $ATTACHMENT_PAYLOAD_TABLE SET filePath = :filePath WHERE messageTid = :msgTid")
    abstract suspend fun updateAttachmentPayLoadFilePathByMsgTid(msgTid: Long, filePath: String?)

    @Query("UPDATE $ATTACHMENT_TABLE SET channelId = :newChannelId WHERE channelId = :oldChannelId")
    abstract suspend fun updateAttachmentsChannelId(oldChannelId: Long, newChannelId: Long): Int

    @Query("UPDATE $ATTACHMENT_TABLE SET filePath = :filePath WHERE messageTid = :msgTid AND type = :type")
    abstract suspend fun updateAttachmentFilePathByMsgTidAndType(msgTid: Long, type: String, filePath: String?)

    @Query("DELETE FROM $ATTACHMENT_TABLE WHERE messageTid = :msgTid AND type = :type")
    abstract suspend fun deleteAttachmentsByMsgTidAndType(msgTid: Long, type: String)

    @VisibleForTesting
    @Insert
    abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)
}

private val untrackedAttachmentTypes = listOf(AttachmentTypeEnum.Link.value, AttachmentTypeEnum.Preview.value)
