package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_SYNC_STATE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelSyncStateEntity

@Dao
internal abstract class ChannelSyncStateDao {

    @Query("SELECT lastSyncedMessageId FROM $CHANNEL_SYNC_STATE_TABLE WHERE channelId = :channelId")
    abstract suspend fun getLastSyncedMessageId(channelId: Long): Long?

    suspend fun upsertChannelSyncState(entity: ChannelSyncStateEntity) {
        if (channelExists(entity.channelId)) {
            upsert(entity)
        }
    }

    @Query("DELETE FROM $CHANNEL_SYNC_STATE_TABLE WHERE channelId = :channelId")
    abstract suspend fun deleteChannelSyncState(channelId: Long)

    @Query("DELETE FROM $CHANNEL_SYNC_STATE_TABLE WHERE channelId IN (:channelIds)")
    abstract suspend fun deleteChannelSyncStates(channelIds: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsert(entity: ChannelSyncStateEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM $CHANNEL_TABLE WHERE chat_id = :channelId)")
    protected abstract suspend fun channelExists(channelId: Long): Boolean
}
