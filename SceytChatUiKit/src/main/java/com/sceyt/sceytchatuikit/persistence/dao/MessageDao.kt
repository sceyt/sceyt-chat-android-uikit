package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MarkerCount
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.persistence.entity.messages.*

@Dao
abstract class MessageDao {

    @Transaction
    open fun insertMessage(messageDb: MessageDb) {
        upsertMessageEntity(messageDb.messageEntity)

        //Delete attachments before insert
        deleteAttachments(listOf(messageDb.messageEntity.tid))

        //Delete reactions before insert
        messageDb.messageEntity.id?.let {
            deleteMessageReactionsAndScores(listOf(it))
        }

        //Insert attachments
        messageDb.attachments?.let {
            insertAttachments(it)
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
    open suspend fun insertMessages(messageDb: List<MessageDb>) {
        upsertMessageEntities(messageDb.map { it.messageEntity })
        //Delete attachments before insert
        deleteAttachments(messageDb.map { it.messageEntity.tid })

        //Delete reactions before insert
        deleteMessageReactionsAndScores(messageDb.mapNotNull { it.messageEntity.id })

        //Insert attachments
        val attachments = messageDb.flatMap { it.attachments ?: arrayListOf() }
        if (attachments.isNotEmpty())
            insertAttachments(attachments)
        //Insert reactions
        val reactions = messageDb.flatMap { it.lastReactions ?: arrayListOf() }
        if (reactions.isNotEmpty())
            insertReactions(reactions.map { it.reaction })
        //Insert reaction scores
        val reactionScores = messageDb.flatMap { it.reactionsScores ?: arrayListOf() }
        if (reactionScores.isNotEmpty())
            insertReactionScores(reactionScores)
    }

    @Transaction
    open fun upsertMessageEntity(messageEntity: MessageEntity) {
        val rowId = insert(messageEntity)
        if (rowId == -1L) {
            updateMessage(messageEntity)
        }
    }

    @Transaction
    open fun upsertMessageEntities(messageEntities: List<MessageEntity>) {
        val rowIds = insertMany(messageEntities)
        val entitiesToUpdate = rowIds.mapIndexedNotNull { index, rowId ->
            if (rowId == -1L) messageEntities[index] else null
        }
        entitiesToUpdate.forEach { updateMessage(it) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insert(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insertMany(messages: List<MessageEntity>): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateMessage(messageEntity: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertReactionScores(reactionScores: List<ReactionScoreEntity>)

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id <:lastMessageId " +
            "order by createdAt desc limit :limit")
    abstract suspend fun getMessages(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @Transaction
    @Query("select * from messages where channelId =:channelId and deliveryStatus =:status " +
            "order by createdAt")
    abstract suspend fun getPendingMessages(channelId: Long, status: DeliveryStatus = DeliveryStatus.Pending): List<MessageDb>

    @Transaction
    @Query("select * from messages where deliveryStatus =:status order by createdAt")
    abstract suspend fun getAllPendingMessages(status: DeliveryStatus = DeliveryStatus.Pending): List<MessageDb>

    @Transaction
    @Query("select * from messages where message_id =:id")
    abstract fun getMessageById(id: Long): MessageDb?

    @Transaction
    @Query("select * from messages where message_id in(:ids)")
    abstract fun getMessageByIds(ids: List<Long>): List<MessageDb>

    @Transaction
    @Query("select * from messages where tid =:tid")
    abstract fun getMessageByTid(tid: Long): MessageDb?

    @Query("update messages set message_id =:serverId, createdAt =:date where tid= :tid")
    abstract suspend fun updateMessageByParams(tid: Long, serverId: Long, date: Long): Int

    @Query("update messages set message_id =:serverId, createdAt =:date, deliveryStatus =:status where tid= :tid")
    abstract suspend fun updateMessageByParams(tid: Long, serverId: Long, date: Long, status: DeliveryStatus): Int

    @Query("update messages set deliveryStatus =:status where message_id in (:ids)")
    abstract fun updateMessageStatus(status: DeliveryStatus, vararg ids: Long): Int

    @Query("update messages set state =:state, body=:body where message_id =:messageId")
    abstract fun updateMessageStateAndBody(messageId: Long, state: MessageState, body: String)

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId")
    abstract fun updateAllMessagesStatusAsRead(channelId: Long, deliveryStatus: DeliveryStatus = DeliveryStatus.Read)

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId and message_id in (:messageIds)")
    abstract suspend fun updateMessagesStatusAsRead(channelId: Long, messageIds: List<Long>, deliveryStatus: DeliveryStatus = DeliveryStatus.Read)


    @Query("update messages set selfMarkers =:markers where channelId =:channelId and message_id =:messageId")
    abstract suspend fun updateMessageSelfMarkers(channelId: Long, messageId: Long, markers: List<String>?)


    @Query("update messages set markerCount =:markerCount where channelId =:channelId and message_id =:messageId")
    abstract suspend fun updateMessageMarkersCount(channelId: Long, messageId: Long, markerCount: List<MarkerCount>?)

    open suspend fun updateMessageSelfMarkersAndMarkerCount(channelId: Long, messageId: Long, marker: String) {
        getMessageById(messageId)?.let { messageDb ->
            val markers: ArrayList<String> = ArrayList(messageDb.messageEntity.selfMarkers
                    ?: arrayListOf())
            markers.add(marker)
            updateMessageSelfMarkers(channelId, messageId, markers.toSet().toList())

            //todo
            /* messageDb.messageEntity.markerCount?.findIndexed { count -> count.key == marker }?.let {
                 val newCount = ArrayList(messageDb.messageEntity.markerCount!!)
                 val markerCount = it.second
                 val newMarkerCount = MarkerCount(markerCount.key, markerCount.count + 1)
                 newCount[it.first] = newMarkerCount
                 updateMessageMarkersCount(channelId, messageId, newCount)
             }*/
        }
    }

    @Query("delete from messages where tid =:tid")
    abstract fun deleteMessageByTid(tid: Long)

    @Query("delete from messages where channelId =:channelId")
    abstract fun deleteAllMessages(channelId: Long)

    @Transaction
    open fun deleteAttachments(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAttachmentsChunked)
    }

    @Transaction
    open fun deleteMessageReactionsAndScores(messageIdes: List<Long>) {
        messageIdes.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAllReactionsAndScores)
    }

    @Query("delete from AttachmentEntity where messageTid in (:messageTides)")
    abstract fun deleteAttachmentsChunked(messageTides: List<Long>)


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
