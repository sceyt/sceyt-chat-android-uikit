package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sceyt.chatuikit.data.models.LoadNearData
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Displayed
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Received
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus.Sent
import com.sceyt.chatuikit.extensions.roundUp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_PAYLOAD_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.AUTO_DELETE_MESSAGES_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOAD_RANGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.POLL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.REACTION_TOTAL_TABLE
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AutoDeleteMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MentionUserMessageLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageIdAndTid
import com.sceyt.chatuikit.persistence.database.entity.messages.PollEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PollOptionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PollVoteEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionTotalEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMarkerEntity
import com.sceyt.chatuikit.persistence.mappers.toAttachmentPayLoad
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Dao
internal abstract class MessageDao {

    @Transaction
    open suspend fun upsertMessage(messageDb: MessageDb): Boolean {
        val forceUpdated = upsertMessageEntity(messageDb.messageEntity)
        insertMessagesPayloads(listOf(messageDb))
        return forceUpdated
    }

    @Transaction
    open suspend fun upsertMessages(messagesDb: List<MessageDb>): List<MessageEntity> {
        if (messagesDb.isEmpty()) return emptyList()
        val forceUpdatedList = upsertMessageEntities(messagesDb.map { it.messageEntity })
        insertMessagesPayloads(messagesDb)
        return forceUpdatedList
    }

    @Transaction
    open suspend fun insertMessagesIgnored(messagesDb: List<MessageDb>) {
        if (messagesDb.isEmpty()) return

        val messageEntities = messagesDb.map { it.messageEntity }
        val rowIds = insertManyIgnored(messageEntities)

        val insertedMessages = messagesDb
            .zip(rowIds)
            .mapNotNull { (msgDb, rowId) -> if (rowId != -1L) msgDb else null }

        if (insertedMessages.isNotEmpty()) {
            insertMessagesPayloads(insertedMessages)
        }
    }

    @Transaction
    open suspend fun insertMessageIgnored(messagesDb: MessageDb) {
        val rowId = insertIgnored(messagesDb.messageEntity)
        if (rowId != -1L)
            insertMessagesPayloads(listOf(messagesDb))
    }

    private suspend fun insertMessagesPayloads(messages: List<MessageDb>) {
        if (messages.isEmpty()) return

        //Delete attachments before insert
        deleteAttachmentsChunked(messages.map { it.messageEntity.tid })

        //Delete reactions scores before insert
        deleteMessageReactionTotalsChunked(messages.mapNotNull { it.messageEntity.id })

        //Delete polls before insert (handles deleted votes, options, etc.)
        deletePollsChunked(messages.map { it.messageEntity.tid })

        //Insert attachments
        insertAttachmentsWithPayloads(*messages.toTypedArray())

        //Insert user markers
        val userMarkers = messages.flatMap { it.userMarkers ?: arrayListOf() }
        if (userMarkers.isNotEmpty())
            insertUserMarkers(userMarkers)

        //Insert reactions
        val reactions = messages.flatMap { it.reactions ?: arrayListOf() }
        if (reactions.isNotEmpty())
            insertReactions(reactions.map { it.reaction })

        //Insert reaction totals
        val reactionTotals = messages.flatMap { it.reactionsTotals ?: arrayListOf() }
        if (reactionTotals.isNotEmpty())
            insertReactionTotals(reactionTotals)

        //Inset mentioned users links
        insertMentionedUsersMessageLinks(*messages.toTypedArray())

        //Insert auto delete messages
        insertAutoDeleteMessage(*messages.toTypedArray())

        //Insert polls
        insertPolls(*messages.toTypedArray())
    }

    /**
     * Upsert message entity.
     * If message exist then update it.
     * If message not exist then insert it.
     * @param messageEntity message entity to upsert
     * @return true if message force inserted
     * */
    private suspend fun upsertMessageEntity(messageEntity: MessageEntity): Boolean {
        val rowId = insertIgnored(messageEntity)
        if (rowId == -1L) {
            val updated = updateMessageIgnored(messageEntity) == 1
            if (!updated) {
                insert(messageEntity)
                SceytLog.d(
                    TAG,
                    "Upsert conflict: message (ID=${messageEntity.id}) failed to update, force inserted."
                )
                return true
            }
        }
        return false
    }

    /**
     * Upsert messages entities.
     * If message exist then update it.
     * If message not exist then insert it.
     * @param messageEntities list of message entities
     * @return list of force inserted message entities
     * */
    private suspend fun upsertMessageEntities(
        messageEntities: List<MessageEntity>,
    ): List<MessageEntity> {
        val rowIds = insertManyIgnored(messageEntities)
        val entitiesToUpdate = rowIds.mapIndexedNotNull { index, rowId ->
            if (rowId == -1L) messageEntities[index] else null
        }
        val count = updateMessagesIgnored(entitiesToUpdate)
        if (count != entitiesToUpdate.size) {
            insertMany(entitiesToUpdate)
            SceytLog.d(
                TAG,
                "Upsert conflict detected: ${entitiesToUpdate.size - count} messages failed to update. " +
                        "Force inserted ${entitiesToUpdate.size} messages."
            )
            return entitiesToUpdate
        }
        return emptyList()
    }

    @Transaction
    open suspend fun upsertMessageEntitiesWithTransaction(messageEntities: List<MessageEntity>) {
        val rowIds = insertManyIgnored(messageEntities)
        val entitiesToUpdate = rowIds.mapIndexedNotNull { index, rowId ->
            if (rowId == -1L) messageEntities[index] else null
        }
        if (entitiesToUpdate.isNotEmpty())
            updateMessagesIgnored(entitiesToUpdate)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIgnored(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertManyIgnored(messages: List<MessageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMany(messages: List<MessageEntity>): List<Long>

    @Update(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun updateMessageIgnored(messageEntity: MessageEntity): Int

    @Update(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun updateMessagesIgnored(messageEntity: List<MessageEntity>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertAttachmentPayLoads(payLoad: List<AttachmentPayLoadEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLinkDetails(payLoad: List<LinkDetailsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertUserMarkers(markers: List<MarkerEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReactionTotals(reactionTotals: List<ReactionTotalEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertMentionedUsersMessageLinks(mentionedUsers: List<MentionUserMessageLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAutoDeletedMessages(entities: List<AutoDeleteMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPendingMarkersIgnored(entities: List<PendingMarkerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollEntities(polls: List<PollEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollOptions(options: List<PollOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertPollVotes(votes: List<PollVoteEntity>)

    @Transaction
    open suspend fun insertPendingMarkers(entities: List<PendingMarkerEntity>) {
        if (entities.isEmpty()) return
        val existMessageIds = getExistMessageByIds(entities.map { it.messageId })
        if (existMessageIds.isEmpty()) return
        val filtered = entities.filter { it.messageId in existMessageIds }
        insertPendingMarkersIgnored(filtered)
    }

    private suspend fun insertAttachmentsWithPayloads(vararg messages: MessageDb) {
        val attachmentPairs = messages.map {
            if (it.attachments.isNullOrEmpty())
                null
            else it.attachments to it
        }.mapNotNull { it }

        if (attachmentPairs.isNotEmpty()) {
            val attachments = mutableListOf<AttachmentEntity>()
            val attachmentPayLoads = mutableListOf<AttachmentPayLoadEntity>()
            val linkDetails = mutableListOf<LinkDetailsEntity>()

            attachmentPairs.forEach { (attachmentsDb, messageDb) ->
                // Add attachments to list
                attachments.addAll(attachmentsDb.map { it.attachmentEntity })
                // Add attachment payloads to list
                attachmentPayLoads.addAll(attachmentsDb.filter {
                    it.attachmentEntity.type != AttachmentTypeEnum.Link.value
                }.map { it.toAttachmentPayLoad(messageDb.messageEntity) })
                // Add link details to list
                linkDetails.addAll(attachmentsDb.mapNotNull { it.linkDetails })
            }

            if (attachments.isNotEmpty())
                insertAttachments(attachments)

            if (attachmentPayLoads.isNotEmpty())
                insertAttachmentPayLoads(attachmentPayLoads)

            if (linkDetails.isNotEmpty())
                insertLinkDetails(linkDetails)
        }
    }

    private suspend fun insertMentionedUsersMessageLinks(vararg messages: MessageDb) {
        val entities = messages.flatMap { item ->
            item.messageEntity.mentionedUsersIds?.map {
                MentionUserMessageLinkEntity(messageTid = item.messageEntity.tid, userId = it)
            } ?: arrayListOf()
        }
        if (entities.isEmpty()) return

        insertMentionedUsersMessageLinks(entities)
    }

    private suspend fun insertAutoDeleteMessage(vararg messages: MessageDb) {
        val filtered = messages.mapNotNull {
            if ((it.messageEntity.autoDeleteAt ?: 0) > 0) {
                val entity = it.messageEntity
                AutoDeleteMessageEntity(
                    messageTid = entity.tid,
                    channelId = entity.channelId,
                    autoDeleteAt = entity.autoDeleteAt ?: 0L
                )
            } else null
        }.takeIf { it.isNotEmpty() } ?: return

        insertAutoDeletedMessages(filtered)
    }

    private suspend fun insertPolls(vararg messages: MessageDb) {
        val pollsToInsert = mutableListOf<PollEntity>()
        val optionsToInsert = mutableListOf<PollOptionEntity>()
        val votesToInsert = mutableListOf<PollVoteEntity>()

        messages.forEach { messageDb ->
            val pollDb = messageDb.poll ?: return@forEach

            // Add poll entity
            pollsToInsert.add(pollDb.pollEntity)

            // Add poll options
            optionsToInsert.addAll(pollDb.options)

            // Add poll votes
            votesToInsert.addAll(pollDb.votes?.map { it.vote }.orEmpty())
        }

        if (pollsToInsert.isNotEmpty()) {
            insertPollEntities(pollsToInsert)
        }

        if (optionsToInsert.isNotEmpty()) {
            insertPollOptions(optionsToInsert)
        }

        if (votesToInsert.isNotEmpty()) {
            insertPollVotes(votesToInsert)
        }
    }

    @Transaction
    open suspend fun insertUserMarkersIfExistMessage(entities: List<MarkerEntity>): List<Long> {
        if (entities.isEmpty()) return emptyList()

        val existMessageIds = getExistMessageByIdsChunked(entities.map { it.messageId }.distinct()).toSet()
        // Filter markers which message exist in db
        val filtered = entities
            .filter { it.messageId in existMessageIds }
            .takeIf { it.isNotEmpty() } ?: return emptyList()

        return filtered.chunked(SQLITE_MAX_VARIABLE_NUMBER).flatMap { insertUserMarkers(it) }
    }

    @Transaction
    @Query(
        """
         SELECT message.*
         FROM $MESSAGE_TABLE AS message
         JOIN $LOAD_RANGE_TABLE AS loadRange 
           ON loadRange.channelId = :channelId
          AND loadRange.startId <= :lastMessageId
          AND loadRange.endId >= :lastMessageId
         WHERE message.channelId = :channelId
           AND message.message_id < :lastMessageId
           AND message.message_id BETWEEN loadRange.startId AND loadRange.endId
           AND NOT message.unList
           AND message.deliveryStatus != $PENDING_STATUS
         ORDER BY message.createdAt DESC, message.tid DESC
         LIMIT :limit
    """
    )
    abstract suspend fun getOldestThenMessages(
        channelId: Long,
        lastMessageId: Long,
        limit: Int
    ): List<MessageDb>

    @Transaction
    @Query(
        """
        SELECT message.*
        FROM $MESSAGE_TABLE AS message
        JOIN $LOAD_RANGE_TABLE AS loadRange
          ON loadRange.channelId = :channelId
         AND loadRange.startId <= :lastMessageId
         AND loadRange.endId >= :lastMessageId
        WHERE message.channelId = :channelId
          AND message.message_id <= :lastMessageId
          AND message.message_id BETWEEN loadRange.startId AND loadRange.endId
          AND NOT message.unList
          AND message.deliveryStatus != $PENDING_STATUS
        ORDER BY message.createdAt DESC, message.tid DESC
        LIMIT :limit
        """
    )
    abstract suspend fun getOldestThenMessagesInclude(
        channelId: Long,
        lastMessageId: Long,
        limit: Int
    ): List<MessageDb>

    @Transaction
    @Query(
        """
        SELECT message.*
        FROM $MESSAGE_TABLE AS message
        JOIN $LOAD_RANGE_TABLE AS loadRange
          ON loadRange.channelId = :channelId
         AND loadRange.startId <= :messageId
         AND loadRange.endId >= :messageId
        WHERE message.channelId = :channelId
          AND message.message_id > :messageId
          AND message.message_id BETWEEN loadRange.startId AND loadRange.endId
          AND NOT message.unList
          AND message.deliveryStatus != $PENDING_STATUS
        ORDER BY message.createdAt, message.tid
        LIMIT :limit
        """
    )
    abstract suspend fun getNewestThenMessage(
        channelId: Long,
        messageId: Long,
        limit: Int
    ): List<MessageDb>

    @Transaction
    @Query(
        """
        SELECT message.*
        FROM $MESSAGE_TABLE AS message
        JOIN $LOAD_RANGE_TABLE AS loadRange
          ON loadRange.channelId = :channelId
         AND loadRange.startId <= :messageId
         AND loadRange.endId >= :messageId
        WHERE message.channelId = :channelId
          AND message.message_id >= :messageId
          AND message.message_id BETWEEN loadRange.startId AND loadRange.endId
          AND NOT message.unList
          AND message.deliveryStatus != $PENDING_STATUS
        ORDER BY message.createdAt, message.tid
        LIMIT :limit
        """
    )
    abstract suspend fun getNewestThenMessageInclude(
        channelId: Long,
        messageId: Long,
        limit: Int
    ): List<MessageDb>

    @Transaction
    open suspend fun getNearMessages(
        channelId: Long,
        messageId: Long,
        limit: Int,
    ): LoadNearData<MessageDb> {
        val oldest = getOldestThenMessagesInclude(channelId, messageId, limit).reversed()
        val includesInOldest = oldest.lastOrNull()?.messageEntity?.id == messageId

        // If the message not exist then return empty list
        if (!includesInOldest)
            return LoadNearData(emptyList(), hasNext = false, hasPrev = false)

        val newest = getNewestThenMessage(channelId, messageId, limit)
        val halfLimit = limit / 2

        val newestDiff = max(halfLimit - newest.size, 0)
        val oldestDiff = max((limit.toDouble() / 2).roundUp() - oldest.size, 0)

        var newMessages = newest.take(halfLimit + oldestDiff)
        val oldMessages = oldest.takeLast(halfLimit + newestDiff)

        if (oldMessages.size < limit && newMessages.size > halfLimit)
            newMessages = newest.take(limit - oldMessages.size)

        val hasPrev = oldest.size > halfLimit
        val hasNext = newest.size > halfLimit

        val data = (oldMessages + newMessages).sortedBy { it.messageEntity.createdAt }
        return LoadNearData(data, hasNext = hasNext, hasPrev)
    }

    @Transaction
    @Query(
        """
        SELECT *
        FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND deliveryStatus = $PENDING_STATUS
        ORDER BY createdAt
        """
    )
    abstract suspend fun getPendingMessages(channelId: Long): List<MessageDb>

    @Query(
        """
        SELECT message_id
        FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND message_id >= :startId
          AND message_id <= :endId
          AND deliveryStatus != $PENDING_STATUS
          AND (createdAt < :dateUntil OR :dateUntil = 0)
          AND NOT unList
        ORDER BY createdAt
        """
    )
    abstract suspend fun getMessagesIdsByRangeIgnoreUnlist(
        channelId: Long,
        startId: Long,
        endId: Long,
        dateUntil: Long = 0L,
    ): List<Long>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $MESSAGE_TABLE
        WHERE deliveryStatus = $PENDING_STATUS
        ORDER BY createdAt
        """
    )
    abstract suspend fun getAllPendingMessages(): List<MessageDb>

    @Transaction
    @Query("SELECT * FROM $MESSAGE_TABLE WHERE message_id = :id")
    abstract suspend fun getMessageById(id: Long): MessageDb?

    @Query("SELECT * FROM $MESSAGE_TABLE WHERE message_id IN (:ids)")
    abstract suspend fun getMessageEntitiesByIds(ids: List<Long>): List<MessageEntity>

    @Query("SELECT message_id AS id, tid FROM $MESSAGE_TABLE WHERE message_id IN (:ids)")
    abstract suspend fun getExistMessagesIdTidByIds(ids: List<Long>): List<MessageIdAndTid>

    @Query("SELECT message_id FROM $MESSAGE_TABLE WHERE message_id IN (:ids)")
    abstract suspend fun getExistMessageByIds(ids: List<Long>): List<Long>

    @Transaction
    open suspend fun getExistMessageByIdsChunked(ids: List<Long>): List<Long> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(SQLITE_MAX_VARIABLE_NUMBER).flatMap { getExistMessageByIds(it) }
    }

    @Transaction
    @Query("SELECT * FROM $MESSAGE_TABLE WHERE tid = :tid")
    abstract suspend fun getMessageByTid(tid: Long): MessageDb?

    @Transaction
    @Query("SELECT * FROM $MESSAGE_TABLE WHERE tid IN (:tIds)")
    abstract suspend fun getMessagesByTid(tIds: List<Long>): List<MessageDb>

    @Transaction
    @Query("SELECT * FROM $MESSAGE_TABLE WHERE deliveryStatus = $PENDING_STATUS AND tid IN (:tIds)")
    abstract suspend fun getPendingMessagesByTIds(tIds: List<Long>): List<MessageDb>

    @Transaction
    @Query("SELECT * FROM $MESSAGE_TABLE WHERE deliveryStatus = $PENDING_STATUS AND tid = :tid")
    abstract suspend fun getPendingMessageByTid(tid: Long): MessageDb?

    @Query("SELECT tid FROM $MESSAGE_TABLE WHERE message_id IN (:ids)")
    abstract suspend fun getMessageTIdsByIds(vararg ids: Long): List<Long>

    @Query("SELECT tid FROM $MESSAGE_TABLE WHERE message_id = :id")
    abstract suspend fun getMessageTidById(id: Long): Long?

    @Query(
        """
        SELECT message_id AS id, tid
        FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND message_id <= :id
          AND deliveryStatus IN (:status)
        """
    )
    abstract suspend fun getMessagesTidAndIdLoverThanByStatus(
        channelId: Long,
        id: Long,
        vararg status: MessageDeliveryStatus
    ): List<MessageIdAndTid>

    @Transaction
    @Query(
        """
        SELECT *
        FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND createdAt >= (SELECT MAX(createdAt) FROM $MESSAGE_TABLE WHERE channelId = :channelId)
        """
    )
    abstract suspend fun getLastMessage(channelId: Long): MessageDb?

    @Query(
        """
        SELECT message_id
        FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND message_id >= (
              SELECT MAX(message_id)
              FROM $MESSAGE_TABLE
              WHERE channelId = :channelId
                AND deliveryStatus != $PENDING_STATUS
          )
        """
    )
    abstract suspend fun getLastSentMessageId(channelId: Long): Long?

    @Query("SELECT COUNT(*) FROM $MESSAGE_TABLE WHERE channelId = :channelId")
    abstract fun getMessagesCountAsFlow(channelId: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM $MESSAGE_TABLE WHERE channelId = :channelId")
    abstract suspend fun getMessagesCount(channelId: Long): Int

    @Query("SELECT message_id FROM $MESSAGE_TABLE WHERE channelId = :channelId ORDER BY createdAt")
    abstract suspend fun getMessagesIds(channelId: Long): List<Long>

    @Query(
        """
        SELECT messageTid
        FROM $AUTO_DELETE_MESSAGES_TABLE
        WHERE channelId = :channelId
          AND autoDeleteAt <= :localTime
        """
    )
    abstract suspend fun getOutdatedMessageTIds(channelId: Long, localTime: Long): List<Long>

    @Query("SELECT EXISTS(SELECT * FROM $MESSAGE_TABLE WHERE message_id = :messageId)")
    abstract suspend fun existsMessageById(messageId: Long): Boolean

    @Query("SELECT EXISTS(SELECT * FROM $MESSAGE_TABLE WHERE tid = :tid)")
    abstract suspend fun existsMessageByTid(tid: Long): Boolean

    @Query("UPDATE $MESSAGE_TABLE SET deliveryStatus = :status WHERE message_id IN (:ids)")
    abstract suspend fun updateMessageStatus(status: MessageDeliveryStatus, vararg ids: Long): Int

    @Transaction
    open suspend fun updateMessageStatusWithBefore(
        channelId: Long,
        status: MessageDeliveryStatus,
        id: Long
    ): List<MessageIdAndTid> {
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

    @Query(
        """
        UPDATE $MESSAGE_TABLE
        SET deliveryStatus = :deliveryStatus
        WHERE channelId = :channelId
          AND incoming
        """
    )
    abstract suspend fun updateAllIncomingMessagesStatusAsRead(
        channelId: Long,
        deliveryStatus: MessageDeliveryStatus = Displayed
    )

    @Query(
        """
        UPDATE $MESSAGE_TABLE
        SET deliveryStatus = :deliveryStatus
        WHERE channelId = :channelId
          AND message_id IN (:messageIds)
        """
    )
    abstract suspend fun updateMessagesStatus(
        channelId: Long,
        messageIds: List<Long>,
        deliveryStatus: MessageDeliveryStatus
    )

    @Query("UPDATE $MESSAGE_TABLE SET channelId = :newChannelId WHERE channelId = :oldChannelId")
    abstract suspend fun updateMessagesChannelId(oldChannelId: Long, newChannelId: Long): Int

    @Query("DELETE FROM $MESSAGE_TABLE WHERE tid = :tid")
    abstract fun deleteMessageByTid(tid: Long)

    @Query("DELETE FROM $MESSAGE_TABLE WHERE tid IN (:tIds)")
    abstract fun deleteMessagesByTid(tIds: List<Long>): Int

    @Query("DELETE FROM $MESSAGE_TABLE WHERE channelId = :channelId")
    abstract suspend fun deleteAllMessagesByChannel(channelId: Long): Int

    @Query("DELETE FROM $MESSAGE_TABLE WHERE channelId IN (:channelIds)")
    abstract suspend fun deleteAllChannelsMessages(channelIds: List<Long>): Int

    @Query(
        """
        DELETE FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND deliveryStatus != $PENDING_STATUS
          AND (createdAt < :deleteUntil OR :deleteUntil = 0)
        """
    )
    abstract suspend fun deleteUntilDateExceptPending(
        channelId: Long,
        deleteUntil: Long,
    ): Int

    @Query(
        """
        DELETE FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND message_id NOT IN (:messageIds)
          AND (createdAt < :deleteUntil OR :deleteUntil = 0)
          AND deliveryStatus != $PENDING_STATUS
          AND NOT unList
        """
    )
    abstract suspend fun deleteNotInMessageIdsUntilDateExceptPending(
        channelId: Long,
        messageIds: List<Long>,
        deleteUntil: Long,
    ): Int

    @Query(
        """
        DELETE FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND createdAt <= :date
          AND deliveryStatus != $PENDING_STATUS
        """
    )
    abstract suspend fun deleteMessagesBeforeDateExceptPending(
        channelId: Long,
        date: Long
    ): Int

    @Query(
        """
        DELETE FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND message_id <= :messageId
          AND deliveryStatus != $PENDING_STATUS
        """
    )
    abstract suspend fun deleteMessagesBeforeIdExceptPending(
        channelId: Long,
        messageId: Long
    ): Int

    @Query(
        """
        DELETE FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
          AND message_id >= :messageId
          AND (createdAt < :deleteUntil OR :deleteUntil = 0)
          AND deliveryStatus != $PENDING_STATUS
        """
    )
    abstract suspend fun deleteMessagesAfterIdUntilDateExceptPending(
        channelId: Long,
        messageId: Long,
        deleteUntil: Long
    ): Int

    @Query("DELETE FROM $MESSAGE_TABLE WHERE channelId = :channelId AND deliveryStatus != $PENDING_STATUS")
    abstract suspend fun deleteAllMessagesExceptPending(channelId: Long)

    @Transaction
    open suspend fun deleteAttachmentsChunked(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach { ids ->
            deleteAttachments(ids)
        }
    }

    @Transaction
    open suspend fun deleteAttachmentsPayloadsChunked(messageTides: List<Long>) {
        messageTides.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach { ids ->
            deleteAttachmentsPayLoad(ids)
        }
    }

    @Transaction
    open suspend fun deleteMessageReactionTotalsChunked(messageIdes: List<Long>) {
        messageIdes.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach { ids ->
            deleteAllReactionsAndTotals(ids)
        }
    }

    @Transaction
    open suspend fun deletePollsChunked(messageTIds: List<Long>) {
        messageTIds.chunked(SQLITE_MAX_VARIABLE_NUMBER).forEach { ids ->
            deletePollsByMessageTides(ids)
        }
    }

    @Query("DELETE FROM $ATTACHMENT_TABLE WHERE messageTid IN (:messageTides)")
    protected abstract suspend fun deleteAttachments(messageTides: List<Long>)

    @Query("DELETE FROM $ATTACHMENT_PAYLOAD_TABLE WHERE messageTid IN (:messageTides)")
    protected abstract suspend fun deleteAttachmentsPayLoad(messageTides: List<Long>)

    @Query("DELETE FROM $POLL_TABLE WHERE messageTid IN (:messageTides)")
    protected abstract suspend fun deletePollsByMessageTides(messageTides: List<Long>)

    @Transaction
    protected open suspend fun deleteAllReactionsAndTotals(messageIds: List<Long>) {
        deleteAllReactionTotalsByMessageId(messageIds)
    }

    @Transaction
    @Query("SELECT * FROM $MESSAGE_TABLE WHERE tid IN (:tids)")
    abstract suspend fun getMessagesByTids(tids: List<Long>): List<MessageDb>

    @Query("DELETE FROM $REACTION_TOTAL_TABLE WHERE messageId IN (:messageId)")
    protected abstract fun deleteAllReactionTotalsByMessageId(messageId: List<Long>)

    private companion object {
        private const val TAG = "MessageDao"
        private const val SQLITE_MAX_VARIABLE_NUMBER: Int = 999
        private const val PENDING_STATUS = 0
    }
}
