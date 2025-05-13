package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.persistence.database.entity.channel.ChanelMemberDb
import com.sceyt.chatuikit.persistence.database.entity.channel.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.entity.user.USER_TABLE

@Dao
internal interface MemberDao {

    @Query("select user_id from $USER_CHAT_LINK_TABLE where chat_id =:channelId and role =:role")
    suspend fun getChannelOwner(channelId: Long, role: String = RoleTypeEnum.Owner.value): String?

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from $USER_CHAT_LINK_TABLE as userChatLink join $USER_TABLE as user " +
            "on userChatLink.user_id = user.user_id " +
            "where chat_id =:channelId " +
            "order by user_id limit :limit offset :offset")
    suspend fun getChannelMembers(channelId: Long, limit: Int, offset: Int): List<ChanelMemberDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from $USER_CHAT_LINK_TABLE as userChatLink " +
            "join $USER_TABLE as user on userChatLink.user_id = user.user_id " +
            "where chat_id =:channelId and role=:role " +
            "order by user_id limit :limit offset :offset")
    suspend fun getChannelMembersWithRole(channelId: Long, limit: Int, offset: Int, role: String): List<ChanelMemberDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from $USER_CHAT_LINK_TABLE as userChatLink join $USER_TABLE as user " +
            "on userChatLink.user_id = user.user_id " +
            "and (firstName like :name || '%' or lastName like :name || '%' or (firstName || ' ' || lastName) " +
            "like :name || '%') " +
            "where chat_id =:channelId")
    suspend fun getChannelMembersByDisplayName(channelId: Long, name: String): List<ChanelMemberDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from $USER_CHAT_LINK_TABLE as links " +
            "join $USER_TABLE as user on links.user_id = user.user_id " +
            "where chat_id =:channelId and links.user_id in (:ids) " +
            "order by user_id")
    suspend fun getChannelMembersByIds(channelId: Long, vararg ids: String): List<ChanelMemberDb>

    @Query("select count(*) from $USER_CHAT_LINK_TABLE where chat_id =:channelId")
    suspend fun getMembersCount(channelId: Long): Int

    @Query("update $USER_CHAT_LINK_TABLE set role =:role where chat_id =:channelId and user_id =:userId")
    suspend fun updateMemberRole(channelId: Long, userId: String, role: String)

    @Transaction
    suspend fun updateOwner(channelId: Long, oldOwnerId: String, newOwnerId: String) {
        updateMemberRole(channelId, oldOwnerId, RoleTypeEnum.Member.value)
        updateMemberRole(channelId, newOwnerId, RoleTypeEnum.Owner.value)
    }

    @Transaction
    suspend fun updateOwner(channelId: Long, newOwnerId: String) {
        getChannelOwner(channelId)?.let {
            updateMemberRole(channelId, it, RoleTypeEnum.Member.value)
        }
        updateMemberRole(channelId, newOwnerId, RoleTypeEnum.Owner.value)
    }

    @Query("select user_id from $USER_CHAT_LINK_TABLE where user_id in (:ids) and chat_id =:channelId")
    fun filterOnlyMembersByIds(channelId: Long, ids: List<String>): List<String>
}