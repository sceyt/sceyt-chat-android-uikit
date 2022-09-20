package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.persistence.entity.messages.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MessageDao {

    @Transaction
    open fun insertMessage(messageDb: MessageDb) {
        insert(messageDb.messageEntity)
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
    open fun insertMessages(messageDb: List<MessageDb>) {
        insertMany(messageDb.map { it.messageEntity })
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insert(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertMany(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAttachments(attachments: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertReactions(reactions: List<ReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertReactionScores(reactionScores: List<ReactionScoreEntity>)

    @Update
    abstract fun updateMessage(messageEntity: MessageEntity)

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id <:lastMessageId " +
            "order by createdAt desc limit :limit")
    abstract suspend fun getMessages(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @Transaction
    @Query("select * from messages where channelId =:channelId and deliveryStatus =:status " +
            "order by createdAt")
    abstract suspend fun getPendingMessages(channelId: Long, status: DeliveryStatus = DeliveryStatus.Pending): List<MessageDb>

    @Query("select * from messages where message_id =:id")
    abstract fun getMessageById(id: Long): Flow<List<MessageEntity>>

    @Query("select * from messages where tid =:tid")
    abstract fun getMessageByTid(tid: Long): MessageEntity?

    @Query("select * from ReactionScoreEntity where messageId =:messageId and reaction_key =:key")
    abstract fun getReactionScore(messageId: Long, key: String): ReactionScoreEntity?

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
    abstract fun updateMessagesStatusAsRead(channelId: Long, messageIds: List<Long>, deliveryStatus: DeliveryStatus = DeliveryStatus.Read)

    @Update
    abstract fun updateReactionScore(reactionScore: ReactionScoreEntity)

    @Query("delete from messages where channelId =:channelId")
    abstract fun deleteAllMessages(channelId: Long)

    @Query("delete from AttachmentEntity where messageId =:messageId")
    abstract fun deleteAttachments(messageId: Long)

    @Query("delete from ReactionEntity where messageId =:messageId")
    abstract fun deleteAllReactions(messageId: Long)

    @Query("delete from ReactionScoreEntity where messageId =:messageId")
    abstract fun deleteAllReactionsScores(messageId: Long)

    @Query("delete from ReactionScoreEntity where id =:id")
    abstract fun deleteReactionScoreById(id: Int)

    @Query("delete from ReactionEntity where messageId =:messageId and reaction_key =:key and fromId =:fromId")
    protected abstract fun deleteReaction(messageId: Long, key: String, fromId: String)

    @Transaction
    open fun deleteReactionAndScore(messageId: Long, key: String, fromId: String) {
        deleteReaction(messageId, key, fromId)
        getReactionScore(messageId, key)?.let {
            if (it.score > 1) {
                it.score--
                updateReactionScore(it)
            } else
                deleteReactionScoreById(it.id)
        }
    }
}
