package com.sceyt.sceytchatuikit.persistence.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.sceytchatuikit.data.models.LoadNearData
import com.sceyt.sceytchatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

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

    @Query("update AttachmentEntity set id =:attachmentId, messageId =:messageId where messageTid =:messageTid and url =:attachmentUrl")
    abstract suspend fun updateAttachmentIdAndMessageId(attachmentId: Long?, messageId: Long, messageTid: Long, attachmentUrl: String?)

    @Query("update AttachmentPayLoad set progressPercent =:progress, transferState =:state where messageTid =:tid")
    abstract fun updateAttachmentTransferDataByMsgTid(tid: Long, progress: Float, state: TransferState)

    @Transaction
    open fun updateAttachmentAndPayLoad(transferData: TransferData) {
        try {
            updateAttachmentByMsgTid(transferData.messageTid, transferData.filePath, transferData.url)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't updateAttachmentByMsgTid: ${e.message}")
        }
        try {
            updateAttachmentPayLoadByMsgTid(transferData.messageTid, transferData.filePath, transferData.url,
                transferData.progressPercent, transferData.state)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't updateAttachmentPayLoadByMsgTid: ${e.message}")
        }
    }

    @Transaction
    open fun updateAttachmentFilePathAndMetadata(tid: Long, filePath: String?, fileSize: Long, metadata: String?) {
        updateAttachmentFilePathByMsgTid(tid, filePath, fileSize, metadata)
        updateAttachmentPayLoadFilePathByMsgTid(tid, filePath)
    }

    @Query("update AttachmentEntity set filePath =:filePath, url =:url where messageTid =:msgTid and type !=:ignoreType")
    abstract fun updateAttachmentByMsgTid(msgTid: Long, filePath: String?, url: String?, ignoreType: String = AttachmentTypeEnum.Link.value())

    @Query("update AttachmentPayLoad set filePath =:filePath, url =:url," +
            "progressPercent= :progress, transferState =:state  where messageTid =:tid")
    abstract fun updateAttachmentPayLoadByMsgTid(tid: Long, filePath: String?, url: String?, progress: Float, state: TransferState)

    @Query("update AttachmentEntity set filePath =:filePath, fileSize =:fileSize, metadata =:metadata " +
            "where messageTid =:msgTid and type !=:ignoreType")
    abstract fun updateAttachmentFilePathByMsgTid(msgTid: Long, filePath: String?, fileSize: Long,
                                                  metadata: String?, ignoreType: String = AttachmentTypeEnum.Link.value())

    @Query("update AttachmentPayLoad set filePath =:filePath where messageTid =:msgTid")
    abstract fun updateAttachmentPayLoadFilePathByMsgTid(msgTid: Long, filePath: String?)


    //TODO: above methods will be removed soon
    @Transaction
    open fun markNotDownloadedAllFileAttachments(): List<Long> {
        val attachments = getAllFileAttachments()
        Log.i("getAllFileAttachments", "result: ${attachments.map { it.url }}")
        val tIds = attachments/*.filter { getMimeType(it.filePath).isNullOrBlank() }*/.map { it.messageTid }
        markNotDownloadedFileAttachments(tIds)
        markNorDownloadedFileAttachmentsPayLoad(tIds)
        return tIds
    }

    @Query("select * from AttachmentEntity  where type =:type and url <> ''")
    abstract fun getAllFileAttachments(type: String = AttachmentTypeEnum.File.value()): List<AttachmentEntity>

    @Query("update AttachmentEntity set filePath = '' where messageTid in (:messageTid)")
    abstract fun markNotDownloadedFileAttachments(messageTid: List<Long>)

    @Query("update AttachmentPayLoad set filePath = '',progressPercent = 0, transferState =:transferState where messageTid in (:messageTid)")
    abstract fun markNorDownloadedFileAttachmentsPayLoad(messageTid: List<Long>, transferState: TransferState = TransferState.PendingDownload)
}