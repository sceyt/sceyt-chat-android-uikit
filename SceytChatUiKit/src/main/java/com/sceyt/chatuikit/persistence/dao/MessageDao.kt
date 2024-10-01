package com.sceyt.chatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.DeliveryStatus.Displayed
import com.sceyt.chat.models.message.DeliveryStatus.Received
import com.sceyt.chat.models.message.DeliveryStatus.Sent
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.persistence.entity.link.LinkDetailsEntity
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.entity.messages.MentionUserMessageLink
import com.sceyt.chatuikit.persistence.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.MessageIdAndTid
import com.sceyt.chatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.entity.messages.ReactionTotalEntity
import com.sceyt.chatuikit.persistence.entity.messages.UserMarkerLink
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.mappers.toAttachmentPayLoad
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

    @Transaction
    open suspend fun insertMessageIgnored(messagesDb: MessageDb) {
        val rowId = insert(messagesDb.messageEntity)
        if (rowId != -1L)
            insertMessagesPayloads(listOf(messagesDb))
    }

    private suspend fun insertMessagesPayloads(messages: List<MessageDb>) {
        if (messages.isEmpty()) return

        //Delete attachments before insert
        deleteAttachmentsChunked(messages.map { it.messageEntity.tid })

        //Delete reactions scores before insert
        deleteMessageReactionTotalsChunked(messages.mapNotNull { it.messageEntity.id })

        //Insert attachments
        val attachmentPairs = messages.map { Pair(it.attachments ?: arrayListOf(), it) }
        if (attachmentPairs.isNotEmpty()) {
            insertAttachments(attachmentPairs.flatMap { it.first.map { attachmentDb -> attachmentDb.attachmentEntity } })
            insertAttachmentPayLoads(attachmentPairs.flatMap { pair ->
                pair.first.filter { it.attachmentEntity.type != AttachmentTypeEnum.Link.value }
                    .map { it.toAttachmentPayLoad(pair.second.messageEntity) }
            })
            insertLinkDetails(attachmentPairs.flatMap { it.first.mapNotNull { attachmentDb -> attachmentDb.linkDetails } })
        }

        //Insert user markers
        val userMarkers = messages.flatMap { it.userMarkers ?: arrayListOf() }
        if (userMarkers.isNotEmpty())
            insertUserMarkersAndLinks(userMarkers)

        //Insert reactions
        val reactions = messages.flatMap { it.reactions ?: arrayListOf() }
        if (reactions.isNotEmpty())
            insertReactions(reactions.map { it.reaction })

        //Insert reaction totals
        val reactionTotals = messages.flatMap { it.reactionsTotals ?: arrayListOf() }
        if (reactionTotals.isNotEmpty())
            insertReactionTotals(reactionTotals)

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
        updateMessages(entitiesToUpdate)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertMany(messages: List<MessageEntity>): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateMessage(messageEntity: MessageEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun updateMessages(messageEntity: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAttachmentPayLoads(payLoad: List<AttachmentPayLoadEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLinkDetails(payLoad: List<LinkDetailsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserMarkers(userMarkers: List<MarkerEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserMarker(userMarker: MarkerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertReactionTotals(reactionTotals: List<ReactionTotalEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMentionedUsersMessageLinks(mentionedUsers: List<MentionUserMessageLink>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insetUserMarkerLinks(links: List<UserMarkerLink>)

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
    open suspend fun insertUserMarkersAndLinks(userMarkers: List<MarkerEntity>) {
        val markersArray = userMarkers.toTypedArray()
        val ids = insertUserMarkers(userMarkers)
        val links = ids.mapIndexed { index, id ->
            val marker = markersArray[index]
            UserMarkerLink(messageId = marker.messageId, markerId = id)
        }
        insetUserMarkerLinks(links)
    }

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from messages join LoadRange as range on range.channelId = :channelId " +
            "and range.startId <= :lastMessageId and range.endId >= :lastMessageId " +
            "where messages.channelId =:channelId and message_id <:lastMessageId " +
            "and (message_id >= range.startId and message_id <= range.endId)" +
            "and not unList and deliveryStatus != $PENDING_STATUS " +
            "group by messages.message_id " +
            "order by createdAt desc, tid desc limit :limit")
    abstract suspend fun getOldestThenMessages(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from messages join LoadRange as range on range.channelId = :channelId " +
            "and range.startId <= :lastMessageId and range.endId >= :lastMessageId " +
            "where messages.channelId =:channelId and message_id <=:lastMessageId " +
            "and (message_id >= range.startId and message_id <= range.endId)" +
            "and not unList and deliveryStatus != $PENDING_STATUS " +
            "group by messages.message_id " +
            "order by createdAt desc, tid desc limit :limit")
    abstract suspend fun getOldestThenMessagesInclude(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from messages join LoadRange as range on range.channelId = :channelId " +
            "and range.startId <= :messageId and range.endId >= :messageId " +
            "where messages.channelId =:channelId and message_id >:messageId " +
            "and (message_id >= range.startId and message_id <= range.endId)" +
            "and not unList and deliveryStatus != $PENDING_STATUS " +
            "group by messages.message_id " +
            "order by createdAt, tid limit :limit")
    abstract suspend fun getNewestThenMessage(channelId: Long, messageId: Long, limit: Int): List<MessageDb>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from messages join LoadRange as range on range.channelId = :channelId " +
            "and range.startId <= :messageId and range.endId >= :messageId " +
            "where messages.channelId =:channelId and message_id >=:messageId " +
            "and (message_id >= range.startId and message_id <= range.endId)" +
            "and not unList and deliveryStatus != $PENDING_STATUS " +
            "group by messages.message_id " +
            "order by createdAt, tid limit :limit")
    abstract suspend fun getNewestThenMessageInclude(channelId: Long, messageId: Long, limit: Int): List<MessageDb>

    @Transaction
    open suspend fun getNearMessages(channelId: Long, messageId: Long, limit: Int): LoadNearData<MessageDb> {
        var newest = getNewestThenMessageInclude(channelId, messageId, limit)
        val includesInNewest = newest.firstOrNull()?.messageEntity?.id == messageId

        newest = if (includesInNewest) { // Remove first message because because it will include in oldest
            newest.toArrayList().apply { removeAt(0) }
        } else emptyList()

        var oldest = getOldestThenMessagesInclude(channelId, messageId, limit).reversed()
        val includesInOldest = oldest.lastOrNull()?.messageEntity?.id == messageId

        if (!includesInOldest)
            oldest = emptyList()

        if (!includesInOldest && !includesInNewest)
            return LoadNearData(emptyList(), hasNext = false, hasPrev = false)

        val newestDiff = max(limit / 2 - newest.size, 0)
        val oldestDiff = max((limit.toDouble() / 2).roundUp() - oldest.size, 0)

        var newMessages = newest.take(limit / 2 + oldestDiff)
        val oldMessages = oldest.takeLast(limit / 2 + newestDiff)

        if (oldMessages.size < limit && newMessages.size > limit / 2)
            newMessages = newest.take(limit - oldMessages.size)

        val hasPrev = oldest.size >= limit / 2
        val hasNext = newest.size >= limit / 2
        return LoadNearData((oldMessages + newMessages).sortedBy { it.messageEntity.createdAt }, hasNext = hasNext, hasPrev)
    }

    @Transaction
    @Query("select * from messages where channelId =:channelId and deliveryStatus = $PENDING_STATUS " +
            "order by createdAt")
    abstract suspend fun getPendingMessages(channelId: Long): List<MessageDb>

    @Transaction
    @Query("select * from messages where deliveryStatus = $PENDING_STATUS order by createdAt")
    abstract suspend fun getAllPendingMessages(): List<MessageDb>

    @Transaction
    @Query("select * from messages where message_id =:id")
    abstract suspend fun getMessageById(id: Long): MessageDb?

    @Query("select message_id as id, tid from messages where message_id in (:ids)")
    abstract suspend fun getExistMessagesIdTidByIds(ids: List<Long>): List<MessageIdAndTid>

    @Query("select message_id from messages where message_id in (:ids)")
    abstract suspend fun getExistMessageByIds(ids: List<Long>): List<Long>

    @Transaction
    @Query("select * from messages where tid =:tid")
    abstract suspend fun getMessageByTid(tid: Long): MessageDb?

    @Transaction
    @Query("select * from messages where tid in (:tIds)")
    abstract suspend fun getMessagesByTid(tIds: List<Long>): List<MessageDb>

    @Transaction
    @Query("select * from messages where deliveryStatus = 0 and tid in (:tIds)")
    abstract suspend fun getPendingMessagesByTIds(tIds: List<Long>): List<MessageDb>

    @Transaction
    @Query("select * from messages where deliveryStatus = 0 and tid =:tid")
    abstract suspend fun getPendingMessageByTid(tid: Long): MessageDb?

    @Query("select tid from messages where message_id in (:ids)")
    abstract suspend fun getMessageTIdsByIds(vararg ids: Long): List<Long>

    @Query("select tid from messages where message_id =:id")
    abstract suspend fun getMessageTidById(id: Long): Long?

    @Query("select message_id as id, tid from messages where channelId =:channelId and message_id <= :id and deliveryStatus in (:status)")
    abstract suspend fun getMessagesTidAndIdLoverThanByStatus(channelId: Long, id: Long, vararg status: DeliveryStatus): List<MessageIdAndTid>

    @Transaction
    @Query("select * from messages where channelId =:channelId and createdAt >= (select max(createdAt) from messages where channelId =:channelId)")
    abstract suspend fun getLastMessage(channelId: Long): MessageDb?

    @Query("select message_id from messages where channelId =:channelId and message_id >= " +
            "(select max(message_id) from messages where channelId =:channelId and deliveryStatus != $PENDING_STATUS)")
    abstract suspend fun getLastSentMessageId(channelId: Long): Long?

    @Query("select exists(select * from messages where message_id =:messageId)")
    abstract suspend fun existsMessageById(messageId: Long): Boolean

    @Query("update messages set deliveryStatus =:status where message_id in (:ids)")
    abstract suspend fun updateMessageStatus(status: DeliveryStatus, vararg ids: Long): Int

    @Transaction
    open suspend fun updateMessageStatusWithBefore(channelId: Long, status: DeliveryStatus, id: Long): List<MessageIdAndTid> {
        val ids = when (status) {
            Displayed -> getMessagesTidAndIdLoverThanByStatus(channelId, id, Sent, Received)
            else -> getMessagesTidAndIdLoverThanByStatus(channelId, id, Sent)
        }.filter { it.id != 0L }

        if (ids.isNotEmpty()) {
            ids.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach { chunkedIds ->
                updateMessageStatus(status, *chunkedIds.mapNotNull { it.id }.toLongArray())
            }
        }

        return ids
    }

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId and incoming")
    abstract suspend fun updateAllIncomingMessagesStatusAsRead(channelId: Long, deliveryStatus: DeliveryStatus = Displayed)

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId =:channelId and message_id in (:messageIds)")
    abstract suspend fun updateMessagesStatus(channelId: Long, messageIds: List<Long>, deliveryStatus: DeliveryStatus)

    @Query("update messages set channelId =:newChannelId where channelId =:oldChannelId")
    abstract suspend fun updateMessagesChannelId(oldChannelId: Long, newChannelId: Long): Int

    @Query("delete from messages where tid =:tid")
    abstract fun deleteMessageByTid(tid: Long)

    @Query("delete from messages where message_id in (:ids)")
    abstract fun deleteMessagesById(ids: List<Long>)

    @Query("delete from messages where channelId =:channelId")
    abstract suspend fun deleteAllMessages(channelId: Long)

    @Query("delete from messages where channelId =:channelId and createdAt <=:date and deliveryStatus != $PENDING_STATUS")
    abstract suspend fun deleteAllMessagesLowerThenDateIgnorePending(channelId: Long, date: Long)

    @Query("delete from messages where channelId =:channelId and deliveryStatus != $PENDING_STATUS")
    abstract suspend fun deleteAllMessagesExceptPending(channelId: Long)

    @Transaction
    open suspend fun deleteAttachmentsChunked(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(this::deleteAttachments)
    }

    @Transaction
    open suspend fun deleteAttachmentsPayloadsChunked(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAttachmentsPayLoad)
    }

    @Transaction
    open suspend fun deleteMessageReactionTotalsChunked(messageIdes: List<Long>) {
        messageIdes.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach(::deleteAllReactionsAndTotals)
    }

    @Query("delete from AttachmentEntity where messageTid in (:messageTides)")
    abstract fun deleteAttachments(messageTides: List<Long>)

    @Query("delete from AttachmentPayLoad where messageTid in (:messageTides)")
    abstract fun deleteAttachmentsPayLoad(messageTides: List<Long>)

    @Transaction
    open fun deleteAllReactionsAndTotals(messageIds: List<Long>) {
        deleteAllReactionTotalsByMessageId(messageIds)
    }

    @Query("delete from ReactionTotalEntity where messageId in (:messageId)")
    abstract fun deleteAllReactionTotalsByMessageId(messageId: List<Long>)

    @Query("delete from ReactionEntity where messageId in (:messageId)")
    protected abstract fun deleteAllReactionsByMessageId(messageId: List<Long>)

    private companion object {
        private const val SQLITE_MAX_VARIABLE_NUMBER: Int = 999
        private const val PENDING_STATUS = 0
    }
}
