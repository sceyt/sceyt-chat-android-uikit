package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChanelMemberDb

@Dao
internal interface MemberDao {

    @Query("SELECT user_id FROM $USER_CHAT_LINK_TABLE WHERE chat_id = :channelId AND role = :role")
    suspend fun getChannelOwner(channelId: Long, role: String = RoleTypeEnum.Owner.value): String?

    @Transaction
    @Query(
        """
        SELECT userChatLink.*
        FROM $USER_CHAT_LINK_TABLE AS userChatLink
        JOIN $USER_TABLE AS user ON userChatLink.user_id = user.user_id
        WHERE chat_id = :channelId
        ORDER BY userChatLink.id
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getChannelMembers(channelId: Long, limit: Int, offset: Int): List<ChanelMemberDb>

    @Transaction
    @Query(
        """
        SELECT userChatLink.*
        FROM $USER_CHAT_LINK_TABLE AS userChatLink
        JOIN $USER_TABLE AS user ON userChatLink.user_id = user.user_id
        WHERE chat_id = :channelId
          AND role = :role
        ORDER BY userChatLink.id
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getChannelMembersWithRole(channelId: Long, limit: Int, offset: Int, role: String): List<ChanelMemberDb>

    @Transaction
    @Query(
        """
        SELECT userChatLink.*
        FROM $USER_CHAT_LINK_TABLE AS userChatLink
        JOIN $USER_TABLE AS user
          ON userChatLink.user_id = user.user_id
         AND (firstName LIKE :name || '%'
           OR lastName LIKE :name || '%'
           OR (firstName || ' ' || lastName) LIKE :name || '%')
        WHERE chat_id = :channelId
        """
    )
    suspend fun getChannelMembersByDisplayName(channelId: Long, name: String): List<ChanelMemberDb>

    @Transaction
    @Query(
        """
        SELECT links.*
        FROM $USER_CHAT_LINK_TABLE AS links
        JOIN $USER_TABLE AS user ON links.user_id = user.user_id
        WHERE chat_id = :channelId
          AND links.user_id IN (:ids)
        ORDER BY user_id
        """
    )
    suspend fun getChannelMembersByIds(channelId: Long, vararg ids: String): List<ChanelMemberDb>

    @Query("SELECT COUNT(*) FROM $USER_CHAT_LINK_TABLE WHERE chat_id = :channelId")
    suspend fun getMembersCount(channelId: Long): Int

    @Query("UPDATE $USER_CHAT_LINK_TABLE SET role = :role WHERE chat_id = :channelId AND user_id = :userId")
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

    @Query("SELECT user_id FROM $USER_CHAT_LINK_TABLE WHERE user_id IN (:ids) AND chat_id = :channelId")
    fun filterOnlyMembersByIds(channelId: Long, ids: List<String>): List<String>
}