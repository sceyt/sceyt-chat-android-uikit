package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionEntity

@Dao
interface ChatUsersReactionDao {

    @Transaction
    suspend fun replaceChannelUserReactions(reactionTotals: List<ChatUserReactionEntity>) {
        deleteChannelsUserReactions(reactionTotals.map { it.channelId })
        insertChannelUserReactions(reactionTotals)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelUserReactions(reactionTotals: List<ChatUserReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelUserReaction(reactionTotals: ChatUserReactionEntity)

    @Transaction
    @Query("select * from ChatUserReactionEntity where channelId =:channelId")
    suspend fun getChannelUserReactions(channelId: Long): List<ChatUserReactionDb>

    @Query("delete from ChatUserReactionEntity where channelId in (:channelIds)")
    suspend fun deleteChannelsUserReactions(channelIds: List<Long>)

    @Query("delete from ChatUserReactionEntity where messageId =:messageId and reaction_key =:key " +
            "and fromId =:fromId and channelId =:channelId")
    suspend fun deleteChannelUserReaction(channelId: Long, messageId: Long, key: String?, fromId: String?)

    @Query("delete from ChatUserReactionEntity where messageId =:messageId and channelId =:channelId")
    suspend fun deleteChannelMessageUserReaction(channelId: Long, messageId: Long)
}
