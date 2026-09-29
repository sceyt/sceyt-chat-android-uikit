package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PINNED_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PinnedMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PinnedMessageEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM $MESSAGE_TABLE WHERE tid = :messageTid)")
    suspend fun messageExists(messageTid: Long): Boolean

    @Transaction
    suspend fun insertIfMessageExists(entity: PinnedMessageEntity) {
        if (messageExists(entity.messageTid)) insert(entity)
    }

    /** [insertIfMessageExists] for several pins in one transaction. */
    @Transaction
    suspend fun insertAllIfMessagesExist(entities: List<PinnedMessageEntity>) {
        entities.forEach { insertIfMessageExists(it) }
    }

    @Transaction
    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE channelId = :channelId
        AND (pinnedUntil IS NULL OR pinnedUntil > :now)
        ORDER BY serverPinId ASC, messageCreatedAt ASC, messageId ASC, messageTid ASC"""
    )
    fun getPinnedMessagesFlow(channelId: Long, now: Long): Flow<List<PinnedMessageDb>>

    @Query("SELECT * FROM $PINNED_MESSAGE_TABLE WHERE messageTid = :messageTid")
    suspend fun getByTid(messageTid: Long): PinnedMessageEntity?

    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE channelId = :channelId
        AND serverPinId NOT IN (:keepServerPinIds)"""
    )
    suspend fun getExcluding(
        channelId: Long,
        keepServerPinIds: List<Long>,
    ): List<PinnedMessageEntity>

    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE messageTid IN (:messageTids)")
    suspend fun deleteByTids(messageTids: List<Long>)
}
