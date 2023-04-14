package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChanelMemberDb

@Dao
interface MembersDao {

    @Query("select user_id from UserChatLink where chat_id =:channelId and role =:role")
    suspend fun getChannelOwner(channelId: Long, role: String = RoleTypeEnum.Owner.toString()): String?

    @Transaction
    @Query("select * from UserChatLink join users on UserChatLink.user_id = users.user_id  where chat_id =:channelId " +
            "order by user_id limit :limit offset :offset")
    suspend fun getChannelMembers(channelId: Long, limit: Int, offset: Int): List<ChanelMemberDb>

    @Transaction
    @Query("select * from UserChatLink join users on UserChatLink.user_id = users.user_id  where chat_id =:channelId " +
            "and role=:role order by user_id limit :limit offset :offset")
    suspend fun getChannelMembersWithRole(channelId: Long, limit: Int, offset: Int, role: String): List<ChanelMemberDb>

    @Transaction
    @Query("select * from UserChatLink join users on UserChatLink.user_id = users.user_id and (firstName like :name || '%'" +
            "or lastName like :name || '%' or (firstName || ' ' || lastName) like :name || '%') " +
            "where chat_id =:channelId")
    suspend fun getChannelMembersByDisplayName(channelId: Long, name: String): List<ChanelMemberDb>

    @Transaction
    @Query("select * from UserChatLink as links join users on links.user_id = users.user_id  where chat_id =:channelId " +
            "and links.user_id in (:ids) order by user_id")
    suspend fun getChannelMembersByIds(channelId: Long, vararg ids: String): List<ChanelMemberDb>

    @Query("select count(*) from UserChatLink where chat_id =:channelId")
    suspend fun getMembersCount(channelId: Long): Int

    @Query("update UserChatLink set role =:role where chat_id =:channelId and user_id =:userId")
    suspend fun updateMemberRole(channelId: Long, userId: String, role: String)

    @Transaction
    suspend fun updateOwner(channelId: Long, oldOwnerId: String, newOwnerId: String) {
        updateMemberRole(channelId, oldOwnerId, RoleTypeEnum.Member.toString())
        updateMemberRole(channelId, newOwnerId, RoleTypeEnum.Owner.toString())
    }

    @Transaction
    suspend fun updateOwner(channelId: Long, newOwnerId: String) {
        getChannelOwner(channelId)?.let {
            updateMemberRole(channelId, it, RoleTypeEnum.Member.toString())
        }
        updateMemberRole(channelId, newOwnerId, RoleTypeEnum.Owner.toString())
    }
}