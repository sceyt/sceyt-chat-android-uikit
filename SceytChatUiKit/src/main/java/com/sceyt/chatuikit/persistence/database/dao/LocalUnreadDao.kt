package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOCAL_CHANNEL_UNREAD_STATE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOCAL_UNREAD_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.LocalChannelUnreadStateEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.LocalUnreadMessageEntity

internal data class LocalUnreadCountsDb(
    val channelId: Long,
    val unreadCount: Long,
    val mentionCount: Long,
)

@Dao
internal abstract class LocalUnreadDao {

    @Query("SELECT * FROM $LOCAL_CHANNEL_UNREAD_STATE_TABLE WHERE channelId = :channelId")
    abstract suspend fun getState(channelId: Long): LocalChannelUnreadStateEntity?

    @Query("SELECT * FROM $LOCAL_CHANNEL_UNREAD_STATE_TABLE WHERE channelId IN (:channelIds)")
    abstract suspend fun getStates(channelIds: List<Long>): List<LocalChannelUnreadStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertState(state: LocalChannelUnreadStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertStates(states: List<LocalChannelUnreadStateEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertUnreadMessagesInternal(
        messages: List<LocalUnreadMessageEntity>
    ): List<Long>

    @Transaction
    open suspend fun insertUnreadMessages(messages: List<LocalUnreadMessageEntity>): Boolean {
        if (messages.isEmpty()) return false
        return insertUnreadMessagesInternal(messages).any { it != -1L }
    }

    @Query(
        """
        SELECT message.channelId AS channelId,
               COUNT(*) AS unreadCount,
               SUM(CASE WHEN message.mentioned = 1 THEN 1 ELSE 0 END) AS mentionCount
        FROM $LOCAL_UNREAD_MESSAGE_TABLE AS message
        JOIN $LOCAL_CHANNEL_UNREAD_STATE_TABLE AS state
          ON state.channelId = message.channelId
        WHERE message.channelId IN (:channelIds)
          AND message.messageId > state.baseUntilMessageId
        GROUP BY message.channelId
        """
    )
    abstract suspend fun getObservedCounts(channelIds: List<Long>): List<LocalUnreadCountsDb>

    @Query("DELETE FROM $LOCAL_UNREAD_MESSAGE_TABLE WHERE channelId = :channelId")
    abstract suspend fun deleteUnreadMessages(channelId: Long)

    @Query(
        """
        DELETE FROM $LOCAL_UNREAD_MESSAGE_TABLE
        WHERE channelId = :channelId
          AND messageId IN (:messageIds)
        """
    )
    abstract suspend fun deleteUnreadMessages(channelId: Long, messageIds: List<Long>)

    @Query("DELETE FROM $LOCAL_CHANNEL_UNREAD_STATE_TABLE WHERE channelId = :channelId")
    abstract suspend fun deleteState(channelId: Long)

    @Query("DELETE FROM $LOCAL_CHANNEL_UNREAD_STATE_TABLE WHERE channelId IN (:channelIds)")
    abstract suspend fun deleteStates(channelIds: List<Long>)

    @Query("DELETE FROM $LOCAL_UNREAD_MESSAGE_TABLE WHERE channelId IN (:channelIds)")
    abstract suspend fun deleteUnreadMessages(channelIds: List<Long>)
}
