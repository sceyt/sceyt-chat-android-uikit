package com.sceyt.chat.ui.persistence.dao

import androidx.room.*
import com.sceyt.chat.ui.persistence.entity.channel.ChannelDb
import com.sceyt.chat.ui.persistence.entity.channel.ChannelEntity
import com.sceyt.chat.ui.persistence.entity.channel.UserChatLink
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannels(vararg channel: ChannelEntity?)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannels(channels: List<ChannelEntity>, userChatLinks: List<UserChatLink>)

    @Query("DELETE FROM channels")
    fun deleteAll()

    @Transaction
    @Query("SELECT * FROM channels ORDER BY lastMessageId DESC LIMIT :limit OFFSET :offset")
    fun getChannelsByLastMessage(limit: Int, offset: Int): List<ChannelDb?>?

    @Transaction
    @Query("SELECT * FROM channels WHERE chat_id = :id")
    fun getChannelById(id: Long): ChannelDb?

    @Transaction
    @Query("SELECT * FROM channels ORDER BY CASE WHEN lastMessageAt == NULL THEN createdAt ELSE lastMessageAt END DESC LIMIT :limit OFFSET :offset")
    fun getChannelsFlow(offset: Int = 0, limit: Int = SceytUIKitConfig.CHANNELS_LOAD_SIZE): Flow<List<ChannelDb>>

    @Transaction
    @Query("SELECT * FROM channels ORDER BY CASE WHEN lastMessageAt == NULL THEN createdAt ELSE lastMessageAt END DESC LIMIT :limit OFFSET :offset")
    fun getChannels(offset: Int = 0, limit: Int = SceytUIKitConfig.CHANNELS_LOAD_SIZE): List<ChannelDb>
}