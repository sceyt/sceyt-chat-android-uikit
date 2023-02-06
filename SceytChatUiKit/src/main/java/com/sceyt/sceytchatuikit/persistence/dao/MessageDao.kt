package com.sceyt.sceytchatuikit.persistence.dao

import android.util.Log
import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.DeliveryStatus.*
import com.sceyt.chat.models.message.MarkerCount
import com.sceyt.sceytchatuikit.data.models.LoadNearData
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.entity.messages.*
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.mappers.toAttachmentPayLoad
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

@Dao
abstract class MessageDao {

    @Transaction
    open suspend fun insertMessage(messageDb: MessageDb) {
        upsertMessageEntity(messageDb.messageEntity)

        //Delete attachments before insert
        deleteAttachmentsChunked(listOf(messageDb.messageEntity.tid))

        //Delete reactions before insert
        messageDb.messageEntity.id?.let {
            deleteMessageReactionsAndScores(listOf(it))
        }

        //Insert attachments
        messageDb.attachments?.let { entities ->
            insertAttachments(entities.map { it.attachmentEntity })
            insertAttachmentPayLoads(entities.map {
                it.toAttachmentPayLoad(messageDb.messageEntity.deliveryStatus)
            })
        }

        //Insert reactions
        messageDb.lastReactions?.let {
            insertReactions(it.map { reactionDb -> reactionDb.reaction })
        }

        //Insert reaction scores
        messageDb.reactionsScores?.let {
            insertReactionScores(it)
        }
    }

    @Transaction
    open suspend fun insertMessages(messagesDb: List<MessageDb>) {
        if (messagesDb.isEmpty()) return
        upsertMessageEntities(messagesDb.map { it.messageEntity })
        //Delete attachments before insert
        deleteAttachmentsChunked(messagesDb.map { it.messageEntity.tid })

        //Delete reactions before insert
        deleteMessageReactionsAndScores(messagesDb.mapNotNull { it.messageEntity.id })

        //Insert attachments
        val attachmentPairs = messagesDb.map { Pair(it.attachments ?: arrayListOf(), it) }
        if (attachmentPairs.isNotEmpty()) {
            insertAttachments(attachmentPairs.flatMap { it.first.map { attachmentDb -> attachmentDb.attachmentEntity } })
            insertAttachmentPayLoads(attachmentPairs.flatMap { pair ->
                pair.first.map { it.toAttachmentPayLoad(pair.second.messageEntity.deliveryStatus) }
            })
        }

        //Insert reactions
        val reactions = messagesDb.flatMap { it.lastReactions ?: arrayListOf() }
        if (reactions.isNotEmpty())
            insertReactions(reactions.map { it.reaction })

        //Insert reaction scores
        val reactionScores = messagesDb.flatMap { it.reactionsScores ?: arrayListOf() }
        if (reactionScores.isNotEmpty())
            insertReactionScores(reactionScores)
    }

    @Transaction
    open suspend fun upsertMessageEntity(messageEntity: MessageEntity) {
        val rowId = insert(messageEntity)
        if (rowId == -1L) {
            updateMessage(messageEntity)
        }
    }

    @Transaction
    open suspend fun upsertMessageEntities(messageEntities: List<MessageEntity>) {
        val rowIds = insertMany(messageEntities)
        val entitiesToUpdate = rowIds.mapIndexedNotNull { index, rowId ->
            if (rowId == -1L) messageEntities[index] else null
        }
        entitiesToUpdate.forEach { updateMessage(it) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertMany(messages: List<MessageEntity>): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateMessage(messageEntity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAttachmentPayLoads(payLoad: List<AttachmentPayLoadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactionScores(reactionScores: List<ReactionScoreEntity>)

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id <:lastMessageId " +
            "order by createdAt desc limit :limit")
    abstract suspend fun getOldestThenMessages(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id >:messageId " +
            "order by createdAt limit :limit")
    abstract suspend fun getNewestThenMessage(channelId: Long, messageId: Long, limit: Int): List<MessageDb>

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id >=:messageId " +
            "order by createdAt limit :limit")
    abstract suspend fun getNewestThenMessageInclude(channelId: Long, messageId: Long, limit: Int): List<MessageDb>

    @Transaction
    open suspend fun getNearMessages(channelId: Long, messageId: Long, limit: Int): LoadNearData<MessageDb> {
        val newest = getNewestThenMessageInclude(channelId, messageId, SceytKitConfig.MESSAGES_LOAD_SIZE / 2 + 1)
        if (newest.isEmpty() || (newest.size == 1 && newest[0].messageEntity.id == messageId))
            return LoadNearData(emptyList(), hasNext = false, hasPrev = false)

        val newMessages = newest.take(SceytKitConfig.MESSAGES_LOAD_SIZE / 1)

        val oldest = getOldestThenMessages(channelId, messageId, limit - newMessages.size)
        val hasPrev = oldest.size == limit - newMessages.size
        val hasNext = newest.size > SceytKitConfig.MESSAGES_LOAD_SIZE / 2
        return LoadNearData((newMessages + oldest).sortedBy { it.messageEntity.createdAt }, hasNext = hasNext, hasPrev)
    }

    @Transaction
    @Query("select * from messages where channelId =:channelId and deliveryStatus =:status " +
            "order by createdAt")
    abstract suspend fun getPendingMessages(channelId: Long, status: DeliveryStatus = Pending): List<MessageDb>

    @Transaction
    @Query("select * from messages where deliveryStatus =:status order by createdAt")
    abstract suspend fun getAllPendingMessages(status: DeliveryStatus = Pending): List<MessageDb>

    @Transaction
    @Query("select * from messages where message_id =:id")
    abstract suspend fun getMessageById(id: Long): MessageDb?

    @Query("select message_id as id, tid from messages where message_id in (:ids)")
    abstract suspend fun getExistMessagesByIds(ids: List<Long>): List<MessageIdAndTid>

    @Transaction
    @Query("select * from messages where tid =:tid")
    abstract suspend fun getMessageByTid(tid: Long): MessageDb?

    @Query("select tid from  messages where message_id in (:ids)")
    abstract suspend fun getMessageTIdsByIds(vararg ids: Long): List<Long>

    @Query("select message_id as id, tid from messages where message_id <= :id and deliveryStatus in (:status)")
    abstract suspend fun getMessagesTidAndIdLoverThanByStatus(id: Long, vararg status: DeliveryStatus): List<MessageIdAndTid>

    @Query("select * from AttachmentPayLoad where messageTid in (:tid)")
    abstract suspend fun getAllAttachmentPayLoadsByMsgTid(vararg tid: Long): List<AttachmentPayLoadEntity>

    @Transaction
    @Query("select * from messages where channelId =:channelId and createdAt >= (select max(createdAt) from messages where channelId =:channelId)")
    abstract suspend fun getLastMessage(channelId: Long): MessageDb?

    @Query("update messages set message_id =:serverId, createdAt =:date where tid= :tid")
    abstract suspend fun updateMessageByParams(tid: Long, serverId: Long, date: Long): Int

    @Query("update messages set message_id =:serverId, createdAt =:date, deliveryStatus =:status where tid= :tid")
    abstract suspend fun updateMessageByParams(tid: Long, serverId: Long, date: Long, status: DeliveryStatus): Int

    @Query("update messages set deliveryStatus =:status where message_id in (:ids)")
    abstract suspend fun updateMessageStatus(status: DeliveryStatus, vararg ids: Long): Int

    @Transaction
    open suspend fun updateMessageStatusWithBefore(status: DeliveryStatus, id: Long): List<MessageIdAndTid> {
        val ids = when (status) {
            Read -> getMessagesTidAndIdLoverThanByStatus(id, Sent, Delivered)
            else -> getMessagesTidAndIdLoverThanByStatus(id, Sent)
        }.filter { it.id != 0L }

        if (ids.isNotEmpty()) {
            ids.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach { chunkedIds ->
                updateMessageStatus(status, *chunkedIds.mapNotNull { it.id }.toLongArray())
            }
        }

        return ids
    }

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId")
    abstract suspend fun updateAllMessagesStatusAsRead(channelId: Long, deliveryStatus: DeliveryStatus = Read)

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId and message_id in (:messageIds)")
    abstract suspend fun updateMessagesStatus(channelId: Long, messageIds: List<Long>, deliveryStatus: DeliveryStatus)

    @Query("update messages set selfMarkers =:markers where channelId =:channelId and message_id =:messageId")
    abstract suspend fun updateMessageSelfMarkers(channelId: Long, messageId: Long, markers: List<String>?)

    @Query("update messages set markerCount =:markerCount where channelId =:channelId and message_id =:messageId")
    abstract suspend fun updateMessageMarkersCount(channelId: Long, messageId: Long, markerCount: List<MarkerCount>?)

    @Transaction
    open suspend fun updateMessageSelfMarkers(channelId: Long, messageId: Long, marker: String) {
        getMessageById(messageId)?.let { messageDb ->
            val selfMarkers = messageDb.messageEntity.selfMarkers?.toArrayList()
            selfMarkers?.add(marker)
            updateMessageSelfMarkers(channelId, messageId, selfMarkers?.toSet()?.toList())
        }
    }

    @Query("update AttachmentPayLoad set progressPercent =:progress, transferState =:state where messageTid =:tid")
    abstract fun updateAttachmentTransferDataByMsgTid(tid: Long, progress: Float, state: TransferState)

    @Transaction
    open fun updateAttachmentAndPayLoad(transferData: TransferData) {
        try {
            updateAttachmentByMsgTid(transferData.messageTid, transferData.filePath, transferData.url)
        } catch (e: Exception) {
            Log.i(TAG, "Couldn't updateAttachmentByMsgTid: ${e.message}")
        }
        try {
            updateAttachmentPayLoadByMsgTid(transferData.messageTid, transferData.filePath, transferData.url,
                transferData.progressPercent, transferData.state)
        } catch (e: Exception) {
            Log.i(TAG, "Couldn't updateAttachmentPayLoadByMsgTid: ${e.message}")
        }
    }

    @Transaction
    open fun updateAttachmentFilePathAndMetadata(tid: Long, filePath: String?, fileSize: Long, metadata: String?) {
        updateAttachmentFilePathByMsgTid(tid, filePath, fileSize, metadata)
        updateAttachmentPayLoadFilePathByMsgTid(tid, filePath)
    }

    @Query("update AttachmentEntity set filePath =:filePath, url =:url where messageTid =:msgTid")
    abstract fun updateAttachmentByMsgTid(msgTid: Long, filePath: String?, url: String?)

    @Query("update AttachmentPayLoad set filePath =:filePath, url =:url," +
            "progressPercent= :progress, transferState =:state  where messageTid =:tid")
    abstract fun updateAttachmentPayLoadByMsgTid(tid: Long, filePath: String?, url: String?, progress: Float, state: TransferState)

    @Query("update AttachmentEntity set filePath =:filePath, fileSize =:fileSize, metadata =:metadata where messageTid =:msgTid")
    abstract fun updateAttachmentFilePathByMsgTid(msgTid: Long, filePath: String?, fileSize: Long, metadata: String?)

    @Query("update AttachmentPayLoad set filePath =:filePath where messageTid =:msgTid")
    abstract fun updateAttachmentPayLoadFilePathByMsgTid(msgTid: Long, filePath: String?)

    @Query("delete from messages where tid =:tid")
    abstract fun deleteMessageByTid(tid: Long)

    @Query("delete from messages where channelId =:channelId")
    abstract suspend fun deleteAllMessages(channelId: Long)

    @Query("delete from messages where channelId =:channelId and deliveryStatus != :deliveryStatus")
    abstract suspend fun deleteAllMessagesExceptPending(channelId: Long, deliveryStatus: DeliveryStatus = Pending)

    @Transaction
    open suspend fun deleteAttachmentsChunked(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(this::deleteAttachments)
    }

    @Transaction
    open suspend fun deleteAttachmentsPayloadsChunked(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAttachmentsPayLoad)
    }

    @Transaction
    open suspend fun deleteMessageReactionsAndScores(messageIdes: List<Long>) {
        messageIdes.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAllReactionsAndScores)
    }

    @Query("delete from AttachmentEntity where messageTid in (:messageTides)")
    abstract fun deleteAttachments(messageTides: List<Long>)

    @Query("delete from AttachmentPayLoad where messageTid in (:messageTides)")
    abstract fun deleteAttachmentsPayLoad(messageTides: List<Long>)

    @Transaction
    open fun deleteAllReactionsAndScores(messageIds: List<Long>) {
        deleteAllReactionScoresByMessageId(messageIds)
        deleteAllReactionsByMessageId(messageIds)
    }

    @Query("delete from ReactionScoreEntity where messageId in (:messageId)")
    abstract fun deleteAllReactionScoresByMessageId(messageId: List<Long>)

    @Query("delete from ReactionEntity where messageId in (:messageId)")
    protected abstract fun deleteAllReactionsByMessageId(messageId: List<Long>)

    private companion object {
        private const val SQLITE_MAX_VARIABLE_NUMBER: Int = 999
    }
}
