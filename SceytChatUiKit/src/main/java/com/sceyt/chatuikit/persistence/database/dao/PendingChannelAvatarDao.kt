package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_CHANNEL_AVATAR_TABLE
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingChannelAvatarEntity

@Dao
internal interface PendingChannelAvatarDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingChannelAvatarEntity)

    @Query("SELECT filePath FROM $PENDING_CHANNEL_AVATAR_TABLE WHERE channelId = :channelId")
    suspend fun getFilePath(channelId: Long): String?

    @Query("DELETE FROM $PENDING_CHANNEL_AVATAR_TABLE WHERE channelId = :channelId")
    suspend fun delete(channelId: Long)
}
