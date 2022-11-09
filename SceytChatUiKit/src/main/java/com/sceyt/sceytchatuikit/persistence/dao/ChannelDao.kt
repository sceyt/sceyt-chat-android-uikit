package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.persistence.entity.ChanelMember
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channel: List<ChannelEntity>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannelsAndLinks(channels: List<ChannelEntity>, userChatLinks: List<UserChatLink>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannelAndLinks(channels: ChannelEntity, userChatLinks: List<UserChatLink>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserChatLinks(userChatLinks: List<UserChatLink>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserChatLink(userChatLink: UserChatLink): Long

    @Transaction
    @Query("select * from channels where myRole is null or myRole !=:ignoreRole " +
            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit :limit offset :offset")
    suspend fun getChannels(limit: Int, offset: Int, ignoreRole: RoleTypeEnum = RoleTypeEnum.None): List<ChannelDb>

    @Transaction
    @Query("select * from channels where subject LIKE '%' || :query || '%' " +
            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit :limit offset :offset")
    fun getChannelsByQuery(limit: Int, offset: Int, query: String): List<ChannelDb>

    @Transaction
    @Query("select * from channels where chat_id =:id")
    suspend fun getChannelById(id: Long): ChannelDb?

    @Query("select user_id from UserChatLink where chat_id =:channelId and role =:role")
    suspend fun getChannelOwner(channelId: Long, role: String = RoleTypeEnum.Owner.toString()): String?

    @Transaction
    @Query("select * from UserChatLink join users on UserChatLink.user_id = users.user_id where chat_id =:channelId " +
            "order by user_id limit :limit offset :offset")
    suspend fun getChannelMembers(channelId: Long, limit: Int, offset: Int): List<ChanelMember>

    @Query("select chat_id from channels where chat_id not in (:ids)")
    suspend fun getNotExistingChannelIdsByIds(ids: List<Long>): List<Long>

    @Query("select chat_id from channels")
    suspend fun getAllChannelsIds(): List<Long>

    @Update
    suspend fun updateChannel(channelEntity: ChannelEntity)

    @Query("update channels set subject =:subject, avatarUrl =:avatarUrl where chat_id= :channelId")
    suspend fun updateChannelSubjectAndAvatarUrl(channelId: Long, subject: String?, avatarUrl: String?)

    @Query("update channels set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt where chat_id= :channelId")
    suspend fun updateLastMessage(channelId: Long, lastMessageTid: Long?, lastMessageAt: Long?)

    @Query("update channels set lastMessageAt =:lastMessageAt, lastReadMessageId =:lastMessageId where chat_id= :channelId")
    fun updateLastMessageWithLastRead(channelId: Long, lastMessageId: Long?, lastMessageAt: Long?)

    @Query("update channels set unreadMessageCount =:count, markedUsUnread = 0 where chat_id= :channelId")
    suspend fun updateUnreadCount(channelId: Long, count: Int)

    @Query("update channels set muted =:muted, muteExpireDate =:muteUntil where chat_id =:channelId")
    suspend fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0)

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

    @Query("delete from channels where chat_id =:channelId")
    suspend fun deleteChannel(channelId: Long)

    @Query("delete from UserChatLink where chat_id =:channelId and user_id in (:userIds)")
    suspend fun deleteUserChatLinks(channelId: Long, vararg userIds: String)

    @Query("delete from UserChatLink where chat_id =:channelId")
    suspend fun deleteChatLinks(channelId: Long)

    @Transaction
    suspend fun deleteChannelAndLinks(channelId: Long) {
        deleteChannel(channelId)
        deleteChatLinks(channelId)
    }
}