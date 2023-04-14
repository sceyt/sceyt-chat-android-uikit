package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionEntity

@Dao
interface ChatUsersReactionDao {

    @Transaction
    suspend fun replaceChannelUserReactions(reactionScores: List<ChatUserReactionEntity>) {
        deleteChannelsUserReactions(reactionScores.map { it.channelId })
        insertChannelUserReactions(reactionScores)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelUserReactions(reactionScores: List<ChatUserReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelUserReaction(reactionScores: ChatUserReactionEntity)

    @Transaction
    @Query("select * from ChatUserReactionEntity where channelId =:channelId")
    suspend fun getChannelUserReactions(channelId: Long): List<ChatUserReactionDb>

    @Query("delete from ChatUserReactionEntity where channelId in(:channelIds)")
    suspend fun deleteChannelsUserReactions(channelIds: List<Long>)

    @Query("delete from ChatUserReactionEntity where messageId =:messageId and reaction_key =:key and fromId =:fromId and channelId =:channelId")
    suspend fun deleteChannelUserReaction(channelId: Long, messageId: Long, key: String?, fromId: String?)
}
