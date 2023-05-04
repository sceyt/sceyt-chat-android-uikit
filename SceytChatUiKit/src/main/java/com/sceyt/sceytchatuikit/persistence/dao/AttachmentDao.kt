package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.sceytchatuikit.data.models.LoadNearData
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentDb
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

@Dao
abstract class AttachmentDao {
    @Transaction
    @Query("select * from AttachmentEntity where channelId =:channelId and id != 0 and id <:attachmentId and type in (:types)" +
            "order by createdAt desc limit :limit")
    abstract suspend fun getOldestThenAttachment(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query("select * from AttachmentEntity where channelId =:channelId and id != 0 and id >:attachmentId and type in (:types)" +
            "order by createdAt limit :limit")
    abstract suspend fun getNewestThenAttachment(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    @Query("select * from AttachmentEntity where channelId =:channelId and id >=:attachmentId and type in (:types)" +
            "order by createdAt limit :limit")
    abstract suspend fun getNewestThenMessageInclude(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): List<AttachmentDb>

    @Transaction
    open suspend fun getNearAttachments(channelId: Long, attachmentId: Long, limit: Int, types: List<String>): LoadNearData<AttachmentDb> {
        val newest = getNewestThenMessageInclude(channelId, attachmentId, SceytKitConfig.ATTACHMENTS_LOAD_SIZE / 2 + 1, types)
        val newMessages = newest.take(SceytKitConfig.ATTACHMENTS_LOAD_SIZE / 1)

        val oldest = getOldestThenAttachment(channelId, attachmentId, limit - newMessages.size, types)
        val hasPrev = oldest.size == limit - newMessages.size
        val hasNext = newest.size > SceytKitConfig.ATTACHMENTS_LOAD_SIZE / 2
        return LoadNearData((newMessages + oldest).sortedBy { it.attachmentEntity.createdAt }, hasNext = hasNext, hasPrev)
    }

    @Query("update AttachmentEntity set id =:attachmentId, messageId =:messageId where messageTid =:messageTid and url =:attachmentUrl")
    abstract suspend fun updateAttachmentIdAndMessageId(attachmentId: Long?, messageId: Long, messageTid: Long, attachmentUrl: String?)
}