package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_PIN_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PINNED_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinDb
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import kotlinx.coroutines.flow.Flow

private const val PENDING_PINS_QUERY =
    "SELECT * FROM $PENDING_PIN_TABLE WHERE channelId = :channelId ORDER BY createdAt ASC"

/** Pin and unpin requests the server has not acknowledged yet. */
@Dao
internal abstract class PendingPinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(entity: PendingPinEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM $MESSAGE_TABLE WHERE tid = :messageTid)")
    protected abstract suspend fun messageExists(messageTid: Long): Boolean

    @Transaction
    open suspend fun replace(entity: PendingPinEntity): PendingPinEntity? {
        if (!messageExists(entity.messageTid)) return null
        return entity.copy(id = insert(entity.copy(id = 0)))
    }

    @Query("SELECT * FROM $PENDING_PIN_TABLE ORDER BY createdAt ASC")
    abstract suspend fun getAll(): List<PendingPinEntity>

    @Query(PENDING_PINS_QUERY)
    abstract suspend fun getByChannel(channelId: Long): List<PendingPinEntity>

    @Query("SELECT * FROM $PENDING_PIN_TABLE WHERE messageTid = :messageTid")
    abstract suspend fun getByTid(messageTid: Long): PendingPinEntity?

    @Query("DELETE FROM $PENDING_PIN_TABLE WHERE id = :id")
    abstract suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM $PENDING_PIN_TABLE WHERE messageTid IN (:messageTids)")
    abstract suspend fun deleteByTids(messageTids: List<Long>)

    @Query("DELETE FROM $PINNED_MESSAGE_TABLE WHERE messageTid = :messageTid")
    protected abstract suspend fun deleteConfirmedPin(messageTid: Long)

    /** Applies the server's unpin while preserving any newer local request. */
    @Transaction
    open suspend fun settleUnpin(sent: PendingPinEntity) {
        deleteConfirmedPin(sent.messageTid)
        deleteById(sent.id)
    }

    @Transaction
    @Query(PENDING_PINS_QUERY)
    abstract fun getPendingPinsFlow(channelId: Long): Flow<List<PendingPinDb>>
}
