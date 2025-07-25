package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.*
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHAT_USER_REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChatUserReactionDb
import com.sceyt.chatuikit.persistence.database.entity.channel.ChatUserReactionEntity

@Dao
internal interface ChatUserReactionDao {

    @Transaction
    suspend fun replaceChannelUserReactions(reaction: List<ChatUserReactionEntity>) {
        deleteChannelsUserReactions(reaction.map { it.channelId })
        insertChannelUserReactions(reaction)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelUserReactions(reaction: List<ChatUserReactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelUserReaction(reaction: ChatUserReactionEntity)

    @Transaction
    @Query("select * from $CHAT_USER_REACTION_TABLE where channelId =:channelId")
    suspend fun getChannelUserReactions(channelId: Long): List<ChatUserReactionDb>

    @Query("delete from $CHAT_USER_REACTION_TABLE where channelId in (:channelIds)")
    suspend fun deleteChannelsUserReactions(channelIds: List<Long>)

    @Query("delete from $CHAT_USER_REACTION_TABLE where messageId =:messageId and reaction_key =:key " +
            "and fromId =:fromId and channelId =:channelId")
    suspend fun deleteChannelUserReaction(channelId: Long, messageId: Long, key: String?, fromId: String?)

    @Query("delete from $CHAT_USER_REACTION_TABLE where messageId =:messageId and channelId =:channelId")
    suspend fun deleteChannelMessageUserReaction(channelId: Long, messageId: Long)
}
