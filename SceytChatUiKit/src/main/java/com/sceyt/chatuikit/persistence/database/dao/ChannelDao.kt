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
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMany(channels: List<ChannelEntity>): List<Long>

    @Transaction
    open suspend fun insertChannelsAndLinks(
            channels: List<ChannelEntity>,
            userChatLinks: List<UserChatLinkEntity>,
    ) {
        insertMany(channels)
        insertUserChatLinks(userChatLinks)
    }

    @Transaction
    open suspend fun insertChannelAndLinks(
            channel: ChannelEntity,
            userChatLinks: List<UserChatLinkEntity>,
    ) {
        insert(channel)
        insertUserChatLinks(userChatLinks)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUserChatLinks(userChatLinks: List<UserChatLinkEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUserChatLink(userChatLink: UserChatLinkEntity): Long

    @Transaction
    @Query("""
        select * from $CHANNEL_TABLE 
            where (case when :onlyMine then userRole <> '' else 1 end) 
            and (not pending or lastMessageTid != 0) 
            and (:isEmptyTypes = 1 or type in (:types)) 
            order by 
            case when pinnedAt > 0 then pinnedAt end desc,
            case when :orderByLastMessage = 1 and lastMessageAt is not null then lastMessageAt end desc,
            createdAt desc 
            limit :limit offset :offset
    """)
    abstract suspend fun getChannels(
            limit: Int,
            offset: Int,
            types: List<String>,
            orderByLastMessage: Boolean,
            onlyMine: Boolean,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
    ): List<ChannelDb>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("""
      select * from $CHANNEL_TABLE as channel  
            left join $USER_CHAT_LINK_TABLE as link on link.chat_id = channel.chat_id 
            where (((subject like '%' || :query || '%' and (not pending or lastMessageTid != 0) and type <> :directType 
            and (case when :onlyMine then channel.userRole <> '' else 1 end)) 
            or (type =:directType and (link.user_id in (:userIds) or isSelf and link.user_id like '%' || :query || '%')))
            and (:isEmptyTypes = 1 or type in (:types))) 
            group by channel.chat_id 
            order by 
            case when pinnedAt > 0 then pinnedAt end desc,
            case when :orderByLastMessage = 1 and lastMessageAt is not null then lastMessageAt end desc, 
            createdAt desc 
            limit :limit offset :offset
    """)
    abstract suspend fun searchChannelsByUserIds(
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
    abstract suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<ChannelDb>

    @Transaction
    @Query("select * from $CHANNEL_TABLE  where chat_id =:id")
    abstract suspend fun getChannelById(id: Long): ChannelDb?

    @Transaction
    @Query("select * from $CHANNEL_TABLE  where chat_id in (:ids)")
    abstract suspend fun getChannelsById(ids: List<Long>): List<ChannelDb>

    @Query("select * from $USER_CHAT_LINK_TABLE where user_id =:userId")
    abstract suspend fun getUserChannelLinksByPeerId(userId: String): List<UserChatLinkEntity>

    @Transaction
    open suspend fun getChannelByPeerId(peerId: String): List<ChannelDb> {
        val links = getUserChannelLinksByPeerId(peerId)
        return getChannelsById(links.map { it.chatId })
    }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from $CHANNEL_TABLE as channel join $USER_CHAT_LINK_TABLE as link " +
            "on link.chat_id = channel.chat_id " +
            "where link.user_id =:peerId and type =:channelType")
    abstract suspend fun getChannelByUserAndType(
            peerId: String,
            channelType: String,
    ): ChannelDb?

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("""
    select * from $CHANNEL_TABLE  
    where chat_id in (
        select link.chat_id from $USER_CHAT_LINK_TABLE as link 
        where link.user_id in (:users)
        group by link.chat_id
        having COUNT(link.user_id) = :userCount
    ) 
    and type = :channelType
""")
    abstract suspend fun getChannelByUsersAndType(
            users: List<String>,
            channelType: String,
            userCount: Int = users.size,
    ): ChannelDb?

    @Transaction
    @Query("select * from $CHANNEL_TABLE  where uri =:uri")
    abstract suspend fun getChannelByUri(uri: String): ChannelDb?

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Transaction
    @Query("select * from $CHANNEL_TABLE  where isSelf = 1")
    abstract suspend fun getSelfChannel(): ChannelDb?

    @Query("select chat_id from $CHANNEL_TABLE  where chat_id not in (:ids) " +
            "and (:isEmptyTypes = 1 or type in (:types))" +
            "and pending != 1")
    abstract suspend fun getNotExistingChannelIdsByIdsAndTypes(
            ids: List<Long>,
            types: List<String>,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
    ): List<Long>

    @Query("select chat_id from $CHANNEL_TABLE  where pending != 1 and (:isEmptyTypes = 1 or type in (:types))")
    abstract suspend fun getAllChannelIdsByTypes(
            types: List<String>,
            isEmptyTypes: Int = if (types.isEmpty()) 1 else 0,
    ): List<Long>

    @Query("select chat_id from $CHANNEL_TABLE ")
    abstract suspend fun getAllChannelsIds(): List<Long>

    @Query("select lastMessageTid from $CHANNEL_TABLE  where chat_id in (:ids)")
    abstract suspend fun getChannelsLastMessageTIds(ids: List<Long>): List<Long>

    @Query("select lastMessageTid from $CHANNEL_TABLE  where chat_id = :id")
    abstract suspend fun getChannelLastMessageTid(id: Long): Long?

    @Transaction
    @Query("""
        select sum(newMessageCount) from $CHANNEL_TABLE 
        where :isEmptyTypes = 1 or type in (:channelTypes)
           """)
    abstract fun getTotalUnreadCountAsFlow(
            channelTypes: List<String>,
            isEmptyTypes: Int = if (channelTypes.isEmpty()) 1 else 0,
    ): Flow<Long?>

    @Query("select count(chat_id) from $CHANNEL_TABLE ")
    abstract suspend fun getAllChannelsCount(): Int

    @Query("select messageRetentionPeriod from $CHANNEL_TABLE  where chat_id = :channelId")
    abstract suspend fun getRetentionPeriodByChannelId(channelId: Long): Long?

    @Update
    abstract suspend fun updateChannel(channelEntity: ChannelEntity): Int

    @Query("update $CHANNEL_TABLE  set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt where chat_id= :channelId")
    abstract suspend fun updateLastMessage(channelId: Long, lastMessageTid: Long?, lastMessageAt: Long?)

    @Query("update $CHANNEL_TABLE  set lastMessageTid =:lastMessageTid, lastMessageAt =:lastMessageAt," +
            "lastDisplayedMessageId =:lastMessageId where chat_id= :channelId")
    abstract suspend fun updateLastMessageWithLastRead(channelId: Long, lastMessageTid: Long?, lastMessageId: Long, lastMessageAt: Long?)

    @Query("update $CHANNEL_TABLE  set newMessageCount =:count, unread = 0 where chat_id= :channelId")
    abstract suspend fun updateUnreadCount(channelId: Long, count: Int)

    @Query("update $CHANNEL_TABLE  set memberCount =:count where chat_id= :channelId")
    abstract suspend fun updateMemberCount(channelId: Long, count: Int)

    @Query("update $CHANNEL_TABLE  set muted =:muted, mutedTill =:muteUntil where chat_id =:channelId")
    abstract suspend fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0)

    @Query("update $CHANNEL_TABLE  set messageRetentionPeriod =:period where chat_id =:channelId")
    abstract suspend fun updateAutoDeleteState(channelId: Long, period: Long)

    @Query("update $CHANNEL_TABLE  set pinnedAt =:pinnedAt where chat_id =:channelId")
    abstract suspend fun updatePinState(channelId: Long, pinnedAt: Long?)

    @Query("delete from $CHANNEL_TABLE  where chat_id =:channelId")
    abstract suspend fun deleteChannel(channelId: Long)

    @Query("delete from $USER_CHAT_LINK_TABLE where chat_id =:channelId and user_id in (:userIds)")
    abstract suspend fun deleteUserChatLinks(channelId: Long, vararg userIds: String)

    @Query("delete from $USER_CHAT_LINK_TABLE where chat_id =:channelId")
    abstract suspend fun deleteChatLinks(channelId: Long)

    @Query("delete from $USER_CHAT_LINK_TABLE where chat_id in (:channelIds)")
    abstract suspend fun deleteChannelsLinks(channelIds: List<Long>)

    @Query("delete from $USER_CHAT_LINK_TABLE where chat_id =:channelId and user_id != :exceptUserId")
    abstract suspend fun deleteChatLinksExceptUser(channelId: Long, exceptUserId: String)

    @Transaction
    open suspend fun deleteChannelAndLinks(channelId: Long) {
        deleteChannel(channelId)
        deleteChatLinks(channelId)
    }

    @Query("delete from $CHANNEL_TABLE  where chat_id in (:ids)")
    abstract suspend fun deleteAllChannelByIds(ids: List<Long>): Int

    @Transaction
    open suspend fun deleteAllChannelsAndLinksById(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            val deletedCount = deleteAllChannelByIds(ids)
            if (deletedCount > 0)
                deleteChannelsLinks(ids)
        }
    }
}