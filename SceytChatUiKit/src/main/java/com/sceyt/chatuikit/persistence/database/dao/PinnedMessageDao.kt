package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PINNED_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.PinScopeEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStates
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.toPinTypeOrdinal
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PinnedMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PinnedMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMany(entities: List<PinnedMessageEntity>)

    /**
     * The display query, shared by the banner and the pinned list.
     *
     * Sorted by [PinnedMessageEntity.serverPinId] ascending so a new pin appends and existing
     * ordinals stay put; the three timeline columns are required tiebreakers, without which
     * rows sharing a pin id (0 for legacy rows, [PinnedMessageEntity.UNKNOWN_SERVER_PIN_ID]
     * for optimistic ones) sort unstably and the list emits phantom moves.
     *
     * Excludes pins whose removal has not yet been acknowledged by the server.
     */
    @Transaction
    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE channelId = :channelId
        AND syncState != ${PinSyncStates.PENDING_UNPIN}
        AND (pinnedUntil IS NULL OR pinnedUntil > :now)
        ORDER BY serverPinId ASC, messageCreatedAt ASC, messageId ASC, messageTid ASC"""
    )
    fun getPinnedMessagesFlow(channelId: Long, now: Long): Flow<List<PinnedMessageDb>>

    @Transaction
    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE channelId = :channelId
        AND syncState != ${PinSyncStates.PENDING_UNPIN}
        AND (pinnedUntil IS NULL OR pinnedUntil > :now)
        ORDER BY serverPinId ASC, messageCreatedAt ASC, messageId ASC, messageTid ASC"""
    )
    suspend fun getPinnedMessages(channelId: Long, now: Long): List<PinnedMessageDb>

    @Query("SELECT * FROM $PINNED_MESSAGE_TABLE WHERE messageTid = :messageTid AND channelId = :channelId")
    suspend fun getByTid(messageTid: Long, channelId: Long): PinnedMessageEntity?

    @Query("SELECT * FROM $PINNED_MESSAGE_TABLE WHERE messageId = :messageId AND channelId = :channelId")
    suspend fun getByMessageId(messageId: Long, channelId: Long): PinnedMessageEntity?

    /** Every unacknowledged intent, oldest attempt first, across all channels. */
    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE syncState = ${PinSyncStates.PENDING_PIN}
        OR syncState = ${PinSyncStates.PENDING_UNPIN}
        ORDER BY lastAttemptAt ASC"""
    )
    suspend fun getAllPending(): List<PinnedMessageEntity>

    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE channelId = :channelId
        AND (syncState = ${PinSyncStates.PENDING_PIN}
        OR syncState = ${PinSyncStates.PENDING_UNPIN})
        ORDER BY lastAttemptAt ASC"""
    )
    suspend fun getPendingByChannel(channelId: Long): List<PinnedMessageEntity>

    /**
     * Reconcile candidates: the channel's synced pins the server did not report.
     * Pending rows are excluded — the server's answer says nothing about an intent it has
     * not been told about yet.
     */
    @Query(
        """SELECT * FROM $PINNED_MESSAGE_TABLE
        WHERE channelId = :channelId
        AND syncState = ${PinSyncStates.SYNCED}
        AND serverPinId NOT IN (:keepServerPinIds)"""
    )
    suspend fun getSyncedExcluding(
        channelId: Long,
        keepServerPinIds: List<Long>
    ): List<PinnedMessageEntity>

    @Query("UPDATE $PINNED_MESSAGE_TABLE SET messageId = :messageId WHERE messageTid = :messageTid")
    suspend fun updateMessageId(messageTid: Long, messageId: Long)

    @Query("UPDATE $PINNED_MESSAGE_TABLE SET retryCount = retryCount + 1, lastAttemptAt = :now WHERE messageTid = :messageTid AND channelId = :channelId")
    suspend fun incrementRetry(messageTid: Long, channelId: Long, now: Long)

    @Query("UPDATE $PINNED_MESSAGE_TABLE SET channelId = :toChannelId WHERE channelId = :fromChannelId")
    suspend fun moveToChannel(fromChannelId: Long, toChannelId: Long)

    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE messageTid = :messageTid AND channelId = :channelId")
    suspend fun deleteByTid(messageTid: Long, channelId: Long)

    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE messageTid IN (:messageTids)")
    suspend fun deleteByTids(messageTids: List<Long>)

    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE channelId = :channelId")
    suspend fun deleteAllByChannel(channelId: Long)

    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE channelId = :channelId AND messageCreatedAt <= :before")
    suspend fun deleteAllByChannelBefore(channelId: Long, before: Long)

    /**
     * Rows whose channel no longer exists. Room's foreign key covers the message side, but
     * nothing links a pin to its channel, so channel deletion needs this sweep.
     */
    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE channelId NOT IN (SELECT chat_id FROM sceyt_channel_table)")
    suspend fun pruneOrphans()

    @Query("SELECT COUNT(*) FROM $PINNED_MESSAGE_TABLE WHERE channelId = :channelId AND syncState != ${PinSyncStates.PENDING_UNPIN}")
    suspend fun countByChannel(channelId: Long): Int

    @Query(
        """UPDATE $MESSAGE_TABLE
        SET pin_isPinned = :isPinned, pin_pinnedTill = :pinnedTill, pin_pinType = :pinType
        WHERE tid = :messageTid"""
    )
    suspend fun updateMessagePinMirror(
        messageTid: Long,
        isPinned: Boolean,
        pinnedTill: Long,
        pinType: Int
    )

    @Query(
        """UPDATE $MESSAGE_TABLE
        SET pin_isPinned = NULL, pin_pinnedTill = NULL, pin_pinType = NULL
        WHERE tid = :messageTid"""
    )
    suspend fun clearMessagePinMirror(messageTid: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM $MESSAGE_TABLE WHERE tid = :messageTid)")
    suspend fun messageExists(messageTid: Long): Boolean

    /**
     * Writes the pin row and its mirror together, and only when the message is actually here.
     *
     * The foreign key to the message is deferred, so a pin written for a message that is not
     * in the table brings the whole transaction down at commit — taking unrelated writes with
     * it — rather than failing on the spot. The message can be missing for ordinary reasons:
     * a realtime pin for a message still being written, or one deleted between the caller
     * reading it and this write. Skipping is safe, because the next channel sync stores the
     * pin again once its message has landed. Same guard the reaction tables use.
     */
    @Transaction
    suspend fun upsertWithMirror(entity: PinnedMessageEntity) {
        if (!messageExists(entity.messageTid)) return
        insert(entity)
        updateMessagePinMirror(
            messageTid = entity.messageTid,
            isPinned = true,
            pinnedTill = entity.pinnedUntil ?: 0L,
            pinType = PinScopeEntity.fromValue(entity.pinScope).toPinTypeOrdinal()
        )
    }

    /**
     * Marks a synced pin for removal and clears the mirror immediately — the bubble must lose
     * its pin when the user asks, not when the server agrees. The row itself stays as a
     * durable intent until the unpin is acknowledged.
     */
    @Transaction
    suspend fun markPendingUnpinWithMirror(messageTid: Long, channelId: Long, now: Long) {
        setSyncState(messageTid, channelId, PinSyncStates.PENDING_UNPIN, now)
        clearMessagePinMirror(messageTid)
    }

    /**
     * Drops the pin row and its mirror by tid alone. [PinnedMessageEntity.messageTid] is the
     * primary key, so this is unambiguous — used by the message-deletion cascade, which does
     * not carry a channel id.
     */
    @Transaction
    suspend fun deleteByTidWithMirror(messageTid: Long) {
        deleteByTids(listOf(messageTid))
        clearMessagePinMirror(messageTid)
    }

    /** Drops the pin row and its mirror together. */
    @Transaction
    suspend fun deleteWithMirror(messageTid: Long, channelId: Long) {
        deleteByTid(messageTid, channelId)
        clearMessagePinMirror(messageTid)
    }

    @Transaction
    suspend fun deleteAllByChannelWithMirrors(channelId: Long) {
        getPinnedMessageTids(channelId).forEach { clearMessagePinMirror(it) }
        deleteAllByChannel(channelId)
    }

    @Query("SELECT messageTid FROM $PINNED_MESSAGE_TABLE WHERE channelId = :channelId")
    suspend fun getPinnedMessageTids(channelId: Long): List<Long>

    /**
     * Clears mirrors in a channel whose pin row is gone, and returns how many it fixed.
     *
     * Only safe to run after a **successful** sweep: "mirror set, no pin row" is a
     * legitimate transient state otherwise — it is what a personal pin from another device
     * looks like, and what the window before the sweep lands looks like.
     */
    @Query(
        """UPDATE $MESSAGE_TABLE
        SET pin_isPinned = NULL, pin_pinnedTill = NULL, pin_pinType = NULL
        WHERE channelId = :channelId
        AND pin_isPinned = 1
        AND tid NOT IN (SELECT messageTid FROM $PINNED_MESSAGE_TABLE WHERE channelId = :channelId)"""
    )
    suspend fun repairPinMirrors(channelId: Long): Int

    @Query(
        """SELECT tid FROM $MESSAGE_TABLE
        WHERE channelId = :channelId
        AND pin_isPinned = 1
        AND tid NOT IN (SELECT messageTid FROM $PINNED_MESSAGE_TABLE WHERE channelId = :channelId)"""
    )
    suspend fun getDriftedMirrorTids(channelId: Long): List<Long>

    @Query("UPDATE $PINNED_MESSAGE_TABLE SET syncState = :syncState, lastAttemptAt = :now WHERE messageTid = :messageTid AND channelId = :channelId")
    suspend fun setSyncState(messageTid: Long, channelId: Long, syncState: Int, now: Long)

    @Query(
        """UPDATE $PINNED_MESSAGE_TABLE
        SET serverPinId = :serverPinId, syncState = ${PinSyncStates.SYNCED}, retryCount = 0
        WHERE messageTid = :messageTid AND channelId = :channelId
        AND syncState = ${PinSyncStates.PENDING_PIN}"""
    )
    suspend fun markSynced(messageTid: Long, channelId: Long, serverPinId: Long): Int
}
