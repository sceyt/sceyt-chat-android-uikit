package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLink
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
    @Query("""
        select * from channels 
            where userRole !=:ignoreRole 
            and (not pending or lastMessageTid != 0) 
            and (:isEmptyTypes = 1 or type in (:types)) 
            order by 
            case when pinnedAt > 0 then pinnedAt end desc,
            case when :orderByLastMessage = 1 and lastMessageAt is not null then lastMessageAt end desc,
            createdAt desc 
            limit :limit offset :offset
    """)
    suspend fun getChannels(
            limit: Int,
            offset: Int,
            types: List<String>,
            orderByLastMessage: Boolean,
            ignoreRole: String = RoleTypeEnum.None.value,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
    ): List<ChannelDb>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("""
      select * from channels 
            left join UserChatLink as link on link.chat_id = channels.chat_id 
            where (((subject like '%' || :query || '%' and (not pending or lastMessageTid != 0) and type <> :directType 
            and (case when :onlyMine then channels.userRole <> '' else 1 end)) 
            or (type =:directType and (link.user_id in (:userIds) or isSelf and link.user_id like '%' || :query || '%')))
            and (:isEmptyTypes = 1 or type in (:types))) 
            group by channels.chat_id 
            order by 
            case when pinnedAt > 0 then pinnedAt end desc,
            case when :orderByLastMessage = 1 and lastMessageAt is not null then lastMessageAt end desc, 
            createdAt desc 
            limit :limit offset :offset
    """)
    suspend fun searchChannelsByUserIds(
            query: String,
            userIds: List<String>,
            limit: Int,
            offset: Int,
            onlyMine: Boolean,
            types: List<String>,
            orderByLastMessage: Boolean,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
            directType: String = ChannelTypeEnum.Direct.value,
    ): List<ChannelDb>

    @Transaction
    @RawQuery
    suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<ChannelDb>

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

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from channels join UserChatLink as link on link.chat_id = channels.chat_id " +
            "where link.user_id =:peerId and type =:channelType")
    suspend fun getChannelByUserAndType(
            peerId: String,
            channelType: String,
    ): ChannelDb?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("""
    select * from channels 
    where chat_id in (
        select link.chat_id from UserChatLink as link 
        where link.user_id in (:users)
        group by link.chat_id
        having COUNT(link.user_id) = :userCount
    ) 
    and type = :channelType
""")
    suspend fun getChannelByUsersAndType(
            users: List<String>,
            channelType: String,
            userCount: Int = users.size,
    ): ChannelDb?

    @Transaction
    @Query("select * from channels where uri =:uri")
    suspend fun getChannelByUri(uri: String): ChannelDb?

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Transaction
    @Query("select * from channels where isSelf = 1")
    suspend fun getSelfChannel(): ChannelDb?

    @Query("select chat_id from channels where chat_id not in (:ids) " +
            "and (:isEmptyTypes = 1 or type in (:types))" +
            "and pending != 1")
    suspend fun getNotExistingChannelIdsByIdsAndTypes(
            ids: List<Long>,
            types: List<String>,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
    ): List<Long>

    @Query("select chat_id from channels where pending != 1 and (:isEmptyTypes = 1 or type in (:types))")
    suspend fun getAllChannelIdsByTypes(
            types: List<String>,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
    ): List<Long>

    @Query("select chat_id from channels")
    suspend fun getAllChannelsIds(): List<Long>

    @Query("select lastMessageTid from channels where chat_id in (:ids)")
    suspend fun getChannelsLastMessageTIds(ids: List<Long>): List<Long>

    @Query("select lastMessageTid from channels where chat_id = :id")
    suspend fun getChannelLastMessageTid(id: Long): Long?

    @Transaction
    @Query("""
        select sum(newMessageCount) from channels
        where :isEmptyTypes = 1 or type in (:channelTypes)
           """)
    fun getTotalUnreadCountAsFlow(
            channelTypes: List<String>,
            isEmptyTypes: Int = if (channelTypes.isEmpty()) 1 else 0,
    ): Flow<Long?>

    @Query("select count(chat_id) from channels")
    suspend fun getAllChannelsCount(): Int

    @Query("select messageRetentionPeriod from channels where chat_id = :channelId")
    suspend fun getRetentionPeriodByChannelId(channelId: Long): Long?

    @Update
    suspend fun updateChannel(channelEntity: ChannelEntity)

    @Query("update channels set subject =:subject, avatarUrl =:avatarUrl where chat_id= :channelId")
    suspend fun updateChannelSubjectAndAvatarUrl(channelId: Long, subject: String?, avatarUrl: String?)

    @Query("update channels set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt where chat_id= :channelId")
    suspend fun updateLastMessage(channelId: Long, lastMessageTid: Long?, lastMessageAt: Long?)

    @Query("update channels set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt," +
            "lastDisplayedMessageId =:lastMessageId where chat_id= :channelId")
    suspend fun updateLastMessageWithLastRead(channelId: Long, lastMessageTid: Long?, lastMessageId: Long?, lastMessageAt: Long?)

    @Query("update channels set newMessageCount =:count, unread = 0 where chat_id= :channelId")
    suspend fun updateUnreadCount(channelId: Long, count: Int)

    @Query("update channels set memberCount =:count where chat_id= :channelId")
    suspend fun updateMemberCount(channelId: Long, count: Int)

    @Query("update channels set muted =:muted, mutedTill =:muteUntil where chat_id =:channelId")
    suspend fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0)

    @Query("update channels set messageRetentionPeriod =:period where chat_id =:channelId")
    suspend fun updateAutoDeleteState(channelId: Long, period: Long)

    @Query("update channels set pinnedAt =:pinnedAt where chat_id =:channelId")
    suspend fun updatePinState(channelId: Long, pinnedAt: Long?)

    @Query("update channels set userRole =:role where chat_id =:channelId")
    suspend fun updateUserRole(channelId: Long, role: String)

    @Query("delete from channels where chat_id =:channelId")
    suspend fun deleteChannel(channelId: Long)

    @Query("delete from UserChatLink where chat_id =:channelId and user_id in (:userIds)")
    suspend fun deleteUserChatLinks(channelId: Long, vararg userIds: String)

    @Query("delete from UserChatLink where chat_id =:channelId")
    suspend fun deleteChatLinks(channelId: Long)

    @Query("delete from UserChatLink")
    suspend fun deleteAllLinks()

    @Query("delete from UserChatLink where chat_id in (:channelIds)")
    suspend fun deleteChannelsLinks(channelIds: List<Long>)

    @Query("delete from UserChatLink where chat_id =:channelId and user_id != :exceptUserId")
    suspend fun deleteChatLinksExceptUser(channelId: Long, exceptUserId: String)

    @Transaction
    suspend fun deleteChannelAndLinks(channelId: Long) {
        deleteChannel(channelId)
        deleteChatLinks(channelId)
    }

    @Query("delete from channels where chat_id in (:ids)")
    suspend fun deleteAllChannelByIds(ids: List<Long>): Int

    @Query("delete from channels where pending != 1")
    suspend fun deleteAllChannels(): Int

    @Transaction
    suspend fun deleteAllChannelsAndLinksByTypes(types: List<String>): List<Long> {
        val ids = getAllChannelIdsByTypes(types)
        if (ids.isNotEmpty()) {
            val deletedCount = deleteAllChannelByIds(ids)
            if (deletedCount > 0)
                deleteChannelsLinks(ids)
        }
        return ids
    }

    @Transaction
    suspend fun deleteAllChannelsAndLinksById(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            val deletedCount = deleteAllChannelByIds(ids)
            if (deletedCount > 0)
                deleteChannelsLinks(ids)
        }
    }
}