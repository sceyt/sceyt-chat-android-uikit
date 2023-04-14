package com.sceyt.sceytchatuikit.persistence.dao

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelDb
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import kotlinx.coroutines.flow.Flow

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
    @Query("select * from channels where role !=:ignoreRole " +
            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit :limit offset :offset")
    suspend fun getChannels(limit: Int, offset: Int, ignoreRole: RoleTypeEnum = RoleTypeEnum.None): List<ChannelDb>

    @Transaction
    @Query("select * from channels where subject LIKE '%' || :query || '%' " +
            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit :limit offset :offset")
    fun getChannelsByQuery(limit: Int, offset: Int, query: String): List<ChannelDb>

    @RawQuery
    suspend fun searchChannelsRaw(query: SimpleSQLiteQuery): List<ChannelDb>

    @Transaction
    @Query("select * from channels where chat_id =:id")
    suspend fun getChannelById(id: Long): ChannelDb?

    @Transaction
    @Query("select * from channels where chat_id in (:ids)")
    suspend fun getChannelsById(ids: List<Long>): List<ChannelDb>

    @Query("select * from UserChatLink where user_id =:userId")
    suspend fun getUserChannelLinksByPeerId(userId: String): List<UserChatLink>

    @Transaction
    suspend fun getChannelByPeerId(peerId: String): List<ChannelDb> {
        val links = getUserChannelLinksByPeerId(peerId)
        return getChannelsById(links.map { it.chatId })
    }

    @Transaction
    @Query("select * from channels join UserChatLink as link on link.chat_id = channels.chat_id " +
            "where link.user_id =:peerId and type =:channelTypeEnum")
    suspend fun getDirectChannel(peerId: String, channelTypeEnum: ChannelTypeEnum = ChannelTypeEnum.Direct): ChannelDb?

    @Query("select chat_id from channels where chat_id not in (:ids)")
    suspend fun getNotExistingChannelIdsByIds(ids: List<Long>): List<Long>

    @Query("select chat_id from channels")
    suspend fun getAllChannelsIds(): List<Long>

    @Transaction
    @Query("select sum(unreadMessageCount) from channels")
    fun getTotalUnreadCountAsFlow(): Flow<Int?>

    @Update
    suspend fun updateChannel(channelEntity: ChannelEntity)

    @Query("update channels set subject =:subject, avatarUrl =:avatarUrl where chat_id= :channelId")
    suspend fun updateChannelSubjectAndAvatarUrl(channelId: Long, subject: String?, avatarUrl: String?)

    @Query("update channels set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt where chat_id= :channelId")
    suspend fun updateLastMessage(channelId: Long, lastMessageTid: Long?, lastMessageAt: Long?)

    @Query("update channels set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt," +
            "lastReadMessageId =:lastMessageId where chat_id= :channelId")
    suspend fun updateLastMessageWithLastRead(channelId: Long, lastMessageTid: Long?, lastMessageId: Long?, lastMessageAt: Long?)

    @Query("update channels set unreadMessageCount =:count, markedUsUnread = 0 where chat_id= :channelId")
    suspend fun updateUnreadCount(channelId: Long, count: Int)

    @Query("update channels set memberCount =:count where chat_id= :channelId")
    suspend fun updateMemberCount(channelId: Long, count: Int)

    @Query("update channels set muted =:muted, muteExpireDate =:muteUntil where chat_id =:channelId")
    suspend fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0)

    @Query("delete from channels where chat_id =:channelId")
    suspend fun deleteChannel(channelId: Long)

    @Query("delete from UserChatLink where chat_id =:channelId and user_id in (:userIds)")
    suspend fun deleteUserChatLinks(channelId: Long, vararg userIds: String)

    @Query("delete from UserChatLink where chat_id =:channelId")
    suspend fun deleteChatLinks(channelId: Long)

    @Query("delete from UserChatLink where chat_id =:channelId and user_id != :exceptUserId")
    suspend fun deleteChatLinksExceptUser(channelId: Long, exceptUserId: String)

    @Transaction
    suspend fun deleteChannelAndLinks(channelId: Long) {
        deleteChannel(channelId)
        deleteChatLinks(channelId)
    }
}