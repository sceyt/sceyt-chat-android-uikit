package com.sceyt.chatuikit.persistence.database.dao

import androidx.annotation.VisibleForTesting
import androidx.room.Dao
import androidx.room.Insert
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
        return LoadNearData(data, hasNext = hasNext, hasPrev)
    }

    @Transaction
    @Query("SELECT * FROM $ATTACHMENT_PAYLOAD_TABLE WHERE messageTid IN (:tid)")
    abstract suspend fun getAllAttachmentPayLoadsByMsgTid(vararg tid: Long): List<AttachmentPayLoadDb>

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

    @Query("UPDATE $ATTACHMENT_TABLE SET filePath = :filePath, url = :url WHERE messageTid = :msgTid AND type != :ignoreType")
    abstract suspend fun updateAttachmentByMsgTid(msgTid: Long, filePath: String?, url: String?, ignoreType: String = AttachmentTypeEnum.Link.value)

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
          AND type != :ignoreType
        """
    )
    abstract suspend fun updateAttachmentFilePathByMsgTid(
        msgTid: Long,
        filePath: String?,
        fileSize: Long,
        metadata: String?,
        ignoreType: String = AttachmentTypeEnum.Link.value
    )

    @Query("UPDATE $ATTACHMENT_PAYLOAD_TABLE SET filePath = :filePath WHERE messageTid = :msgTid")
    abstract suspend fun updateAttachmentPayLoadFilePathByMsgTid(msgTid: Long, filePath: String?)

    @VisibleForTesting
    @Insert
    abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)
}