package com.sceyt.chat.ui.persistence.dao

import androidx.room.*
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.persistence.entity.messages.MessageDb
import com.sceyt.chat.ui.persistence.entity.messages.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(messages: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageEntity>)

    @Transaction
    @Query("select * from messages where channelId =:channelId and message_id >= :lastMessageId " +
            "order by message_id desc limit :limit")
    fun getMessages(channelId: Long, lastMessageId: Long, limit: Int): List<MessageDb>

    @Query("select * from messages where message_id=:id")
    fun getMessageById(id: Long): Flow<List<MessageEntity>>

    @Query("select * from messages where tid=:tid")
    fun getMessageByTid(tid: Long): MessageEntity?

    @Query("update messages set message_id = :serverId and createdAt=:date where tid= :tid")
    fun updateMessageServerIdAndDate(tid: Long, serverId: Long, date: Long): Int

    @Query("update messages set deliveryStatus =:status where message_id in (:ids)")
    fun updateMessageStatus(status: DeliveryStatus, vararg ids: Long): Int

    @Query("update messages set state =:state and body=:body where message_id=:messageId")
    fun updateMessageStateAndBody(messageId: Long, state: MessageState, body: String)

    @Query("update messages set deliveryStatus =:deliveryStatus where channelId=:channelId")
    fun updateAllMessagesStatusAsRead(channelId: Long, deliveryStatus: DeliveryStatus = DeliveryStatus.Read)

    @Query("delete from messages where channelId=:channelId")
    fun deleteAllMessages(channelId: Long)
}