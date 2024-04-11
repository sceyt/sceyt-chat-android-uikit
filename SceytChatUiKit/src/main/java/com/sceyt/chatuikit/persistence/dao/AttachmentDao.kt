package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentPayLoadDb
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig

@Dao
abstract class AttachmentDao {
    @Transaction
    @Query("select * from AttachmentEntity where channelId =:channelId and id != 0 and id <:attachmentId and type in (:types)" +
            "order by createdAt desc, id desc limit :limit")
    abstract suspend fun getOldestThenAttachment(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query("select * from AttachmentEntity where channelId =:channelId and id != 0 and id >:attachmentId and type in (:types)" +
            "order by createdAt, id limit :limit")
    abstract suspend fun getNewestThenAttachment(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query("select * from AttachmentEntity where channelId =:channelId and id >=:attachmentId and type in (:types)" +
            "order by createdAt, id limit :limit")
    abstract suspend fun getNewestThenMessageInclude(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    open suspend fun getNearAttachments(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): LoadNearData<AttachmentDb> {
        val newest = getNewestThenMessageInclude(channelId, attachmentId, SceytKitConfig.ATTACHMENTS_LOAD_SIZE / 2 + 1, types)
        val newMessages = newest.take(SceytKitConfig.ATTACHMENTS_LOAD_SIZE / 1)

        val oldest = getOldestThenAttachment(channelId, attachmentId, limit - newMessages.size, types).reversed()
        val hasPrev = oldest.size == limit - newMessages.size
        val hasNext = newest.size > SceytKitConfig.ATTACHMENTS_LOAD_SIZE / 2
        return LoadNearData(oldest + newMessages, hasNext = hasNext, hasPrev)
    }

    @Transaction
    @Query("select * from AttachmentPayLoad where messageTid in (:tid)")
    abstract suspend fun getAllAttachmentPayLoadsByMsgTid(vararg tid: Long): List<AttachmentPayLoadDb>

    @Query("select * from AttachmentEntity where type =:type and url <> ''")
    abstract fun getAllFileAttachments(type: String = AttachmentTypeEnum.File.value()): List<AttachmentEntity>

    @Query("update AttachmentEntity set id =:attachmentId, messageId =:messageId where messageTid =:messageTid and url =:attachmentUrl")
    abstract suspend fun updateAttachmentIdAndMessageId(attachmentId: Long?, messageId: Long, messageTid: Long, attachmentUrl: String?)

    @Query("update AttachmentPayLoad set progressPercent =:progress, transferState =:state where messageTid =:tid")
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

    @Query("update AttachmentEntity set filePath =:filePath, url =:url where messageTid =:msgTid and type !=:ignoreType")
    abstract suspend fun updateAttachmentByMsgTid(msgTid: Long, filePath: String?, url: String?, ignoreType: String = AttachmentTypeEnum.Link.value())

    @Query("update AttachmentPayLoad set filePath =:filePath, url =:url," +
            "progressPercent= :progress, transferState =:state  where messageTid =:tid")
    abstract suspend fun updateAttachmentPayLoadByMsgTid(tid: Long, filePath: String?, url: String?, progress: Float, state: TransferState)

    @Query("update AttachmentEntity set filePath =:filePath, fileSize =:fileSize, metadata =:metadata " +
            "where messageTid =:msgTid and type !=:ignoreType")
    abstract suspend fun updateAttachmentFilePathByMsgTid(msgTid: Long, filePath: String?, fileSize: Long,
                                                          metadata: String?, ignoreType: String = AttachmentTypeEnum.Link.value())

    @Query("update AttachmentPayLoad set filePath =:filePath where messageTid =:msgTid")
    abstract suspend fun updateAttachmentPayLoadFilePathByMsgTid(msgTid: Long, filePath: String?)
}