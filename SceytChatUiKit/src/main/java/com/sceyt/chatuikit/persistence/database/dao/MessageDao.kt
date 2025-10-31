package com.sceyt.chatuikit.persistence.database.dao

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
    open suspend fun insertUserMarkersIfExistMessage(entities: List<MarkerEntity>) {
        val existMessageIds = getExistMessageByIds(entities.map { it.messageId })
        // Filter markers which message exist in db
        val filtered = entities
            .filter { it.messageId in existMessageIds }
            .takeIf { it.isNotEmpty() } ?: return

        insertUserMarkers(filtered)
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        "select * from $MESSAGE_TABLE as message " +
                "join $LOAD_RANGE_TABLE as loadRange on loadRange.channelId = :channelId " +
                "and loadRange.startId <= :lastMessageId and loadRange.endId >= :lastMessageId " +
                "where message.channelId =:channelId and message_id <:lastMessageId " +
                "and (message_id >= loadRange.startId and message_id <= loadRange.endId)" +
                "and not unList and deliveryStatus != $PENDING_STATUS " +
                "group by message.message_id " +
                "order by createdAt desc, tid desc limit :limit"
    )
    abstract suspend fun getOldestThenMessages(
        channelId: Long,
        lastMessageId: Long,
        limit: Int
    ): List<MessageDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        "select * from $MESSAGE_TABLE as message " +
                "join $LOAD_RANGE_TABLE as loadRange on loadRange.channelId = :channelId " +
                "and loadRange.startId <= :lastMessageId and loadRange.endId >= :lastMessageId " +
                "where message.channelId =:channelId and message_id <=:lastMessageId " +
                "and (message_id >= loadRange.startId and message_id <= loadRange.endId)" +
                "and not unList and deliveryStatus != $PENDING_STATUS " +
                "group by message.message_id " +
                "order by createdAt desc, tid desc limit :limit"
    )
    abstract suspend fun getOldestThenMessagesInclude(
        channelId: Long,
        lastMessageId: Long,
        limit: Int
    ): List<MessageDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        "select * from $MESSAGE_TABLE as message " +
                "join $LOAD_RANGE_TABLE as loadRange on loadRange.channelId = :channelId " +
                "and loadRange.startId <= :messageId and loadRange.endId >= :messageId " +
                "where message.channelId =:channelId and message_id >:messageId " +
                "and (message_id >= loadRange.startId and message_id <= loadRange.endId)" +
                "and not unList and deliveryStatus != $PENDING_STATUS " +
                "group by message.message_id " +
                "order by createdAt, tid limit :limit"
    )
    abstract suspend fun getNewestThenMessage(
        channelId: Long,
        messageId: Long,
        limit: Int
    ): List<MessageDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query(
        "select * from $MESSAGE_TABLE as message " +
                "join $LOAD_RANGE_TABLE as loadRange on loadRange.channelId = :channelId " +
                "and loadRange.startId <= :messageId and loadRange.endId >= :messageId " +
                "where message.channelId =:channelId and message_id >=:messageId " +
                "and (message_id >= loadRange.startId and message_id <= loadRange.endId)" +
                "and not unList and deliveryStatus != $PENDING_STATUS " +
                "group by message.message_id " +
                "order by createdAt, tid limit :limit"
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
        "select * from $MESSAGE_TABLE where channelId =:channelId and deliveryStatus = $PENDING_STATUS " +
                "order by createdAt"
    )
    abstract suspend fun getPendingMessages(channelId: Long): List<MessageDb>

    @Transaction
    @Query("select * from $MESSAGE_TABLE where deliveryStatus = $PENDING_STATUS order by createdAt")
    abstract suspend fun getAllPendingMessages(): List<MessageDb>

    @Transaction
    @Query("select * from $MESSAGE_TABLE where message_id =:id")
    abstract suspend fun getMessageById(id: Long): MessageDb?

    @Query("select message_id as id, tid from $MESSAGE_TABLE where message_id in (:ids)")
    abstract suspend fun getExistMessagesIdTidByIds(ids: List<Long>): List<MessageIdAndTid>

    @Query("select message_id from $MESSAGE_TABLE where message_id in (:ids)")
    abstract suspend fun getExistMessageByIds(ids: List<Long>): List<Long>

    @Transaction
    @Query("select * from $MESSAGE_TABLE where tid =:tid")
    abstract suspend fun getMessageByTid(tid: Long): MessageDb?

    @Transaction
    @Query("select * from $MESSAGE_TABLE where tid in (:tIds)")
    abstract suspend fun getMessagesByTid(tIds: List<Long>): List<MessageDb>

    @Transaction
    @Query("select * from $MESSAGE_TABLE where deliveryStatus = 0 and tid in (:tIds)")
    abstract suspend fun getPendingMessagesByTIds(tIds: List<Long>): List<MessageDb>

    @Transaction
    @Query("select * from $MESSAGE_TABLE where deliveryStatus = 0 and tid =:tid")
    abstract suspend fun getPendingMessageByTid(tid: Long): MessageDb?

    @Query("select tid from $MESSAGE_TABLE where message_id in (:ids)")
    abstract suspend fun getMessageTIdsByIds(vararg ids: Long): List<Long>

    @Query("select message_id as id, tid from $MESSAGE_TABLE where channelId =:channelId and message_id <= :id and deliveryStatus in (:status)")
    abstract suspend fun getMessagesTidAndIdLoverThanByStatus(
        channelId: Long,
        id: Long,
        vararg status: DeliveryStatus
    ): List<MessageIdAndTid>

    @Transaction
    @Query("select * from $MESSAGE_TABLE where channelId =:channelId and createdAt >= (select max(createdAt) from $MESSAGE_TABLE where channelId =:channelId)")
    abstract suspend fun getLastMessage(channelId: Long): MessageDb?

    @Query(
        "select message_id from $MESSAGE_TABLE where channelId =:channelId and message_id >= " +
                "(select max(message_id) from $MESSAGE_TABLE where channelId =:channelId and deliveryStatus != $PENDING_STATUS)"
    )
    abstract suspend fun getLastSentMessageId(channelId: Long): Long?

    @Query("select count(*) from $MESSAGE_TABLE where channelId = :channelId")
    abstract fun getMessagesCountAsFlow(channelId: Long): Flow<Long?>

    @Query(
        "select messageTid from $AUTO_DELETE_MESSAGES_TABLE" +
                " where channelId = :channelId and autoDeleteAt <= :localTime"
    )
    abstract suspend fun getOutdatedMessageTIds(channelId: Long, localTime: Long): List<Long>

    @Query("select exists(select * from $MESSAGE_TABLE where message_id =:messageId)")
    abstract suspend fun existsMessageById(messageId: Long): Boolean

    @Query("select exists(select * from $MESSAGE_TABLE where tid =:tid)")
    abstract suspend fun existsMessageByTid(tid: Long): Boolean

    @Query("update $MESSAGE_TABLE set deliveryStatus =:status where message_id in (:ids)")
    abstract suspend fun updateMessageStatus(status: DeliveryStatus, vararg ids: Long): Int

    @Transaction
    open suspend fun updateMessageStatusWithBefore(
        channelId: Long,
        status: DeliveryStatus,
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

    @Query("update $MESSAGE_TABLE set deliveryStatus =:deliveryStatus where channelId =:channelId and incoming")
    abstract suspend fun updateAllIncomingMessagesStatusAsRead(
        channelId: Long,
        deliveryStatus: DeliveryStatus = Displayed
    )

    @Query("update $MESSAGE_TABLE set deliveryStatus =:deliveryStatus where channelId =:channelId and message_id in (:messageIds)")
    abstract suspend fun updateMessagesStatus(
        channelId: Long,
        messageIds: List<Long>,
        deliveryStatus: DeliveryStatus
    )

    @Query("update $MESSAGE_TABLE set channelId =:newChannelId where channelId =:oldChannelId")
    abstract suspend fun updateMessagesChannelId(oldChannelId: Long, newChannelId: Long): Int

    @Query("delete from $MESSAGE_TABLE where tid =:tid")
    abstract fun deleteMessageByTid(tid: Long)

    @Query("delete from $MESSAGE_TABLE where tid in (:tIds)")
    abstract fun deleteMessagesByTid(tIds: List<Long>)

    @Query("delete from $MESSAGE_TABLE where channelId =:channelId")
    abstract suspend fun deleteAllMessagesByChannel(channelId: Long)

    @Query("delete from $MESSAGE_TABLE where channelId in (:channelIds)")
    abstract suspend fun deleteAllChannelsMessages(channelIds: List<Long>)

    @Query("delete from $MESSAGE_TABLE where channelId =:channelId and createdAt <=:date and deliveryStatus != $PENDING_STATUS")
    abstract suspend fun deleteAllMessagesLowerThenDateIgnorePending(channelId: Long, date: Long)

    @Query("delete from $MESSAGE_TABLE where channelId =:channelId and deliveryStatus != $PENDING_STATUS")
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

    @Query("delete from $ATTACHMENT_TABLE where messageTid in (:messageTides)")
    protected abstract suspend fun deleteAttachments(messageTides: List<Long>)

    @Query("delete from $ATTACHMENT_PAYLOAD_TABLE where messageTid in (:messageTides)")
    protected abstract suspend fun deleteAttachmentsPayLoad(messageTides: List<Long>)

    @Query("delete from $POLL_TABLE WHERE messageTid IN (:messageTides)")
    protected abstract suspend fun deletePollsByMessageTides(messageTides: List<Long>)

    @Transaction
    protected open suspend fun deleteAllReactionsAndTotals(messageIds: List<Long>) {
        deleteAllReactionTotalsByMessageId(messageIds)
    }

    @Query("delete from $REACTION_TOTAL_TABLE where messageId in (:messageId)")
    protected abstract fun deleteAllReactionTotalsByMessageId(messageId: List<Long>)

    private companion object {
        private const val TAG = "MessageDao"
        private const val SQLITE_MAX_VARIABLE_NUMBER: Int = 999
        private const val PENDING_STATUS = 0
    }
}
