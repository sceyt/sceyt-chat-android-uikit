package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.DeliveryStatus.*
import com.sceyt.chat.models.message.MarkerTotal
import com.sceyt.sceytchatuikit.data.models.LoadNearData
import com.sceyt.sceytchatuikit.extensions.roundUp
import com.sceyt.sceytchatuikit.persistence.entity.messages.*
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.mappers.toAttachmentPayLoad
import kotlin.math.max

@Dao
abstract class MessageDao {

    @Transaction
    open suspend fun upsertMessage(messageDb: MessageDb) {
        upsertMessageEntity(messageDb.messageEntity)
        insertMessagesPayloads(listOf(messageDb))
    }

    @Transaction
    open suspend fun upsertMessages(messagesDb: List<MessageDb>) {
        if (messagesDb.isEmpty()) return
        upsertMessageEntities(messagesDb.map { it.messageEntity })
        insertMessagesPayloads(messagesDb)
    }

    @Transaction
    open suspend fun insertMessagesIgnored(messagesDb: List<MessageDb>) {
        if (messagesDb.isEmpty()) return

        val entities = messagesDb.map { it.messageEntity }
        val rowIds = insertMany(entities)
        val insertedMessages = rowIds.mapIndexedNotNull { index, rowId ->
            if (rowId != -1L) messagesDb.firstOrNull { it.messageEntity.tid == entities[index].tid } else null
        }

        insertMessagesPayloads(insertedMessages)
    }

    private suspend fun insertMessagesPayloads(messages: List<MessageDb>) {
        if (messages.isEmpty()) return

        //Delete attachments before insert
        deleteAttachmentsChunked(messages.map { it.messageEntity.tid })

        //Delete reactions scores before insert
        deleteMessageReactionScoresChunked(messages.mapNotNull { it.messageEntity.id })

        //Insert attachments
        val attachmentPairs = messages.map { Pair(it.attachments ?: arrayListOf(), it) }
        if (attachmentPairs.isNotEmpty()) {
            insertAttachments(attachmentPairs.flatMap { it.first.map { attachmentDb -> attachmentDb.attachmentEntity } })
            insertAttachmentPayLoads(attachmentPairs.flatMap { pair ->
                pair.first.map { it.toAttachmentPayLoad(pair.second.messageEntity) }
            })
        }

        //Insert reactions
        val reactions = messages.flatMap { it.reactions ?: arrayListOf() }
        if (reactions.isNotEmpty())
            insertReactions(reactions.map { it.reaction })

        //Insert reaction scores
        val reactionScores = messages.flatMap { it.reactionsScores ?: arrayListOf() }
        if (reactionScores.isNotEmpty())
            insertReactionScores(reactionScores)

        //Inset mentioned users links
        insertMentionedUsersMessageLinks(*messages.map { it.messageEntity }.toTypedArray())
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
    abstract suspend fun insertReactionScores(reactionScores: List<ReactionTotalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMentionedUsersMessageLinks(mentionedUsers: List<MentionUserMessageLink>)

    private suspend fun insertMentionedUsersMessageLinks(vararg messageEntity: MessageEntity) {
        val entities = messageEntity.flatMap { entity ->
            entity.mentionedUsersIds?.map {
                MentionUserMessageLink(messageTid = entity.tid, userId = it)
            } ?: arrayListOf()
        }
        if (entities.isEmpty()) return

        insertMentionedUsersMessageLinks(entities)
    }

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id <:lastMessageId " +
            "and not isParentMessage order by createdAt desc, tid desc limit :limit")
    abstract suspend fun getOldestThenMessages(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id <=:lastMessageId " +
            "and not isParentMessage order by createdAt desc, tid desc limit :limit")
    abstract suspend fun getOldestThenMessagesInclude(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id >:messageId and not isParentMessage " +
            "order by createdAt, tid limit :limit")
    abstract suspend fun getNewestThenMessage(channelId: Long, messageId: Long, limit: Int): List<MessageDb>

    @Transaction
    open suspend fun getNearMessages(channelId: Long, messageId: Long, limit: Int): LoadNearData<MessageDb> {
        val newest = getNewestThenMessage(channelId, messageId, limit)

        if (newest.isEmpty() || (newest.size == 1 && newest[0].messageEntity.id == messageId))
            return LoadNearData(emptyList(), hasNext = false, hasPrev = false)

        val oldest = getOldestThenMessagesInclude(channelId, messageId, limit).reversed()

        val newestDiff = max(limit / 2 - newest.size, 0)
        val oldestDiff = max((limit.toDouble() / 2).roundUp() - oldest.size, 0)

        val newMessages = newest.take(limit / 2 + oldestDiff)
        val oldMessages = oldest.takeLast(limit / 2 + 1 + newestDiff)

        val hasPrev = oldest.size > limit / 2
        val hasNext = newest.size > limit / 2
        return LoadNearData((oldMessages + newMessages).sortedBy { it.messageEntity.createdAt }, hasNext = hasNext, hasPrev)
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

    @Query("select exists(select * from messages where message_id =:messageId)")
    abstract suspend fun existsMessageById(messageId: Long): Boolean

    @Query("update messages set message_id =:serverId, createdAt =:date where tid= :tid")
    abstract suspend fun updateMessageByParams(tid: Long, serverId: Long, date: Long): Int

    @Query("update messages set message_id =:serverId, createdAt =:date, deliveryStatus =:status where tid= :tid")
    abstract suspend fun updateMessageByParams(tid: Long, serverId: Long, date: Long, status: DeliveryStatus): Int

    @Query("update messages set deliveryStatus =:status where message_id in (:ids)")
    abstract suspend fun updateMessageStatus(status: DeliveryStatus, vararg ids: Long): Int

    @Transaction
    open suspend fun updateMessageStatusWithBefore(status: DeliveryStatus, id: Long): List<MessageIdAndTid> {
        val ids = when (status) {
            Displayed -> getMessagesTidAndIdLoverThanByStatus(id, Sent, Received)
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
    abstract suspend fun updateAllMessagesStatusAsRead(channelId: Long, deliveryStatus: DeliveryStatus = Displayed)

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId and message_id in (:messageIds)")
    abstract suspend fun updateMessagesStatus(channelId: Long, messageIds: List<Long>, deliveryStatus: DeliveryStatus)

    @Query("update messages set userMarkers =:markers where channelId =:channelId and message_id =:messageId")
    abstract suspend fun updateMessageSelfMarkers(channelId: Long, messageId: Long, markers: List<String>?)

    @Query("update messages set markerCount =:markerCount where channelId =:channelId and message_id =:messageId")
    abstract suspend fun updateMessageMarkersCount(channelId: Long, messageId: Long, markerCount: List<MarkerTotal>?)

    @Transaction
    open suspend fun updateMessageSelfMarkers(channelId: Long, messageId: Long, marker: String) {
        getMessageById(messageId)?.let { messageDb ->
            val selfMarkers = messageDb.messageEntity.userMarkers?.toArrayList()
            selfMarkers?.add(marker)
            updateMessageSelfMarkers(channelId, messageId, selfMarkers?.toSet()?.toList())
        }
    }

    @Query("update messages set channelId =:newChannelId where channelId =:oldChannelId")
    abstract suspend fun updateMessagesChannelId(oldChannelId: Long, newChannelId: Long): Int

    @Query("delete from messages where tid =:tid")
    abstract fun deleteMessageByTid(tid: Long)

    @Query("delete from messages where channelId =:channelId")
    abstract suspend fun deleteAllMessages(channelId: Long)

    @Query("delete from messages where channelId =:channelId and createdAt <=:date and deliveryStatus != :ignoreStatus")
    abstract suspend fun deleteAllMessagesLowerThenDateIgnorePending(channelId: Long, date: Long, ignoreStatus: DeliveryStatus = Pending)

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
    open suspend fun deleteMessageReactionScoresChunked(messageIdes: List<Long>) {
        messageIdes.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAllReactionsAndScores)
    }

    @Query("delete from AttachmentEntity where messageTid in (:messageTides)")
    abstract fun deleteAttachments(messageTides: List<Long>)

    @Query("delete from AttachmentPayLoad where messageTid in (:messageTides)")
    abstract fun deleteAttachmentsPayLoad(messageTides: List<Long>)

    @Transaction
    open fun deleteAllReactionsAndScores(messageIds: List<Long>) {
        deleteAllReactionScoresByMessageId(messageIds)
    }

    @Query("delete from ReactionTotalEntity where messageId in (:messageId)")
    abstract fun deleteAllReactionScoresByMessageId(messageId: List<Long>)

    @Query("delete from ReactionEntity where messageId in (:messageId)")
    protected abstract fun deleteAllReactionsByMessageId(messageId: List<Long>)

    private companion object {
        private const val SQLITE_MAX_VARIABLE_NUMBER: Int = 999
    }
}
