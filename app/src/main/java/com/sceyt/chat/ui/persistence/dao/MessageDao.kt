package com.sceyt.chat.ui.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.persistence.entity.messages.AttachmentEntity
import com.sceyt.chat.ui.persistence.entity.messages.MessageDb
import com.sceyt.chat.ui.persistence.entity.messages.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MessageDao {

    @Transaction
    open fun insertMessage(messageDb: MessageDb) {
        insert(messageDb.messageEntity)
        messageDb.attachments?.let {
            insertAttachments(it)
        }
    }

    @Transaction
    open fun insertMessages(messageDb: List<MessageDb>) {
        insertMany(messageDb.map { it.messageEntity })
        val attachments = messageDb.flatMap { it.attachments ?: arrayListOf() }
        if (attachments.isNotEmpty())
            insertAttachments(attachments)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insert(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertMany(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAttachments(attachments: List<AttachmentEntity>)

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

    @Query("delete from messages where channelId =:channelId")
    abstract fun deleteAllMessages(channelId: Long)

    @Query("delete from AttachmentEntity where messageId =:messageId")
    abstract fun deleteAttachments(messageId: Long)
}