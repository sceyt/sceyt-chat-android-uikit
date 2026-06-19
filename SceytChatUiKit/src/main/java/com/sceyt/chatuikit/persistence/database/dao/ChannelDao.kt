package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    @Query(
        """
        SELECT *
        FROM $CHANNEL_TABLE
        WHERE (NOT :onlyMine OR userRole <> '')
          AND (NOT pending OR lastMessageTid != 0)
          AND (:typesEmpty OR type IN (:types))
        ORDER BY
          CASE WHEN pinnedAt > 0 THEN pinnedAt END DESC,
          CASE WHEN :orderByLastMessage AND lastMessageAt IS NOT NULL THEN lastMessageAt END DESC,
          createdAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getChannels(
        limit: Int,
        offset: Int,
        types: List<String>,
        orderByLastMessage: Boolean,
        onlyMine: Boolean,
        typesEmpty: Boolean = types.isEmpty()
    ): List<ChannelDb>

    @Transaction
    @RawQuery
    abstract suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<ChannelDb>

    @Transaction
    @Query("""SELECT * FROM $CHANNEL_TABLE WHERE chat_id = :id""")
    abstract suspend fun getChannelById(id: Long): ChannelDb?

    @Transaction
    @Query("""SELECT * FROM $CHANNEL_TABLE WHERE chat_id IN (:ids)""")
    abstract suspend fun getChannelsById(ids: List<Long>): List<ChannelDb>

    @Query("""SELECT * FROM $USER_CHAT_LINK_TABLE WHERE user_id = :userId""")
    abstract suspend fun getUserChannelLinksByPeerId(userId: String): List<UserChatLinkEntity>

    @Transaction
    open suspend fun getChannelByPeerId(peerId: String): List<ChannelDb> {
        val links = getUserChannelLinksByPeerId(peerId)
        return getChannelsById(links.map { it.chatId })
    }

    @Transaction
    @Query(
        """
        SELECT channel.* FROM $CHANNEL_TABLE AS channel
        JOIN $USER_CHAT_LINK_TABLE AS link ON link.chat_id = channel.chat_id
        WHERE link.user_id = :peerId
          AND type = :channelType
        """
    )
    abstract suspend fun getChannelByUserAndType(
        peerId: String,
        channelType: String,
    ): ChannelDb?

    @Transaction
    @Query(
        """
        SELECT * FROM $CHANNEL_TABLE
        WHERE chat_id IN (
            SELECT link.chat_id
            FROM $USER_CHAT_LINK_TABLE AS link
            WHERE link.user_id IN (:users)
            GROUP BY link.chat_id
            HAVING COUNT(link.user_id) = :userCount
        )
        AND type = :channelType
        """
    )
    abstract suspend fun getChannelByUsersAndType(
        users: List<String>,
        channelType: String,
        userCount: Int = users.size,
    ): ChannelDb?

    @Transaction
    @Query("""SELECT * FROM $CHANNEL_TABLE WHERE uri = :uri""")
    abstract suspend fun getChannelByUri(uri: String): ChannelDb?

    @Query("""UPDATE $CHANNEL_TABLE SET uri = :uri WHERE chat_id = :channelId""")
    abstract suspend fun updateUri(channelId: Long, uri: String?)

    @Transaction
    @Query("""SELECT * FROM $CHANNEL_TABLE WHERE isSelf = 1""")
    abstract suspend fun getSelfChannel(): ChannelDb?

    @Transaction
    @Query("""SELECT * FROM $CHANNEL_TABLE WHERE pending = 1 AND type = :type""")
    abstract suspend fun getPendingChannelsByType(type: String): List<ChannelDb>

    @Query(
        """
        SELECT chat_id
        FROM $CHANNEL_TABLE
        WHERE chat_id NOT IN (:ids)
          AND (:typesEmpty OR type IN (:types))
          AND (NOT :onlyMine OR userRole <> '')
          AND pending != 1
        """
    )
    abstract suspend fun getNotExistingChannelIdsByIdsAndTypes(
        ids: List<Long>,
        types: List<String>,
        onlyMine: Boolean,
        typesEmpty: Boolean = types.isEmpty()
    ): List<Long>

    @Query(
        """
        SELECT chat_id
        FROM $CHANNEL_TABLE
        WHERE pending != 1
          AND (:typesEmpty OR type IN (:types))
          AND (NOT :onlyMine OR userRole <> '')
        """
    )
    abstract suspend fun getAllChannelIdsByTypes(
        types: List<String>,
        onlyMine: Boolean,
        typesEmpty: Boolean = types.isEmpty()
    ): List<Long>

    @Query("""SELECT chat_id FROM $CHANNEL_TABLE""")
    abstract suspend fun getAllChannelsIds(): List<Long>

    @Query("""SELECT lastMessageTid FROM $CHANNEL_TABLE WHERE chat_id IN (:ids)""")
    abstract suspend fun getChannelsLastMessageTIds(ids: List<Long>): List<Long>

    @Query("""SELECT lastMessageTid FROM $CHANNEL_TABLE WHERE chat_id = :id""")
    abstract suspend fun getChannelLastMessageTid(id: Long): Long?

    fun getTotalUnreadCountAsFlow(channelTypes: List<String>): Flow<Long> {
        return if (channelTypes.isEmpty()) {
            getTotalUnreadCountAsFlow()
        } else {
            getTotalUnreadCountByTypesAsFlow(channelTypes)
        }.map { it ?: 0L }
    }

    @Query("SELECT SUM(newMessageCount) FROM $CHANNEL_TABLE")
    protected abstract fun getTotalUnreadCountAsFlow(): Flow<Long?>

    @Query(
        """
        SELECT SUM(newMessageCount)
        FROM $CHANNEL_TABLE
        WHERE type IN (:channelTypes)
        """
    )
    protected abstract fun getTotalUnreadCountByTypesAsFlow(channelTypes: List<String>): Flow<Long?>

    @Query("""SELECT COUNT(chat_id) FROM $CHANNEL_TABLE""")
    abstract suspend fun getAllChannelsCount(): Int

    @Query("""SELECT messageRetentionPeriod FROM $CHANNEL_TABLE WHERE chat_id = :channelId""")
    abstract suspend fun getRetentionPeriodByChannelId(channelId: Long): Long?

    @Update
    abstract suspend fun updateChannel(channelEntity: ChannelEntity): Int

    @Query(
        """UPDATE $CHANNEL_TABLE SET lastMessageTid = :lastMessageTid, lastMessageAt = :lastMessageAt WHERE chat_id = :channelId"""
    )
    abstract suspend fun updateLastMessage(
        channelId: Long,
        lastMessageTid: Long?,
        lastMessageAt: Long?
    )

    @Query(
        """UPDATE $CHANNEL_TABLE SET 
            lastMessageTid = :lastMessageTid,
            lastMessageAt = :lastMessageAt,
            newMessageCount = :newMessageCount
           WHERE chat_id = :channelId"""
    )
    abstract suspend fun updateLastMessageAndNewMessageCount(
        channelId: Long,
        lastMessageTid: Long?,
        lastMessageAt: Long?,
        newMessageCount: Long
    )

    @Query(
        """
        UPDATE $CHANNEL_TABLE
        SET lastMessageTid = :lastMessageTid,
            lastMessageAt = :lastMessageAt,
            lastDisplayedMessageId = :lastMessageId
        WHERE chat_id = :channelId
        """
    )
    abstract suspend fun updateLastMessageWithLastRead(
        channelId: Long,
        lastMessageTid: Long?,
        lastMessageId: Long,
        lastMessageAt: Long?
    )

    @Query("""UPDATE $CHANNEL_TABLE SET newMessageCount = :count, unread = 0 WHERE chat_id = :channelId""")
    abstract suspend fun updateUnreadCount(channelId: Long, count: Int)

    @Query("""UPDATE $CHANNEL_TABLE SET memberCount = :count WHERE chat_id = :channelId""")
    abstract suspend fun updateMemberCount(channelId: Long, count: Int)

    @Query("""UPDATE $CHANNEL_TABLE SET muted = :muted, mutedTill = :muteUntil WHERE chat_id = :channelId""")
    abstract suspend fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0)

    @Query("""UPDATE $CHANNEL_TABLE SET messageRetentionPeriod = :period WHERE chat_id = :channelId""")
    abstract suspend fun updateAutoDeleteState(channelId: Long, period: Long)

    @Query("""UPDATE $CHANNEL_TABLE SET pinnedAt = :pinnedAt WHERE chat_id = :channelId""")
    abstract suspend fun updatePinState(channelId: Long, pinnedAt: Long?)

    @Query("""DELETE FROM $CHANNEL_TABLE WHERE chat_id = :channelId""")
    abstract suspend fun deleteChannel(channelId: Long)

    @Query("""DELETE FROM $USER_CHAT_LINK_TABLE WHERE chat_id = :channelId AND user_id IN (:userIds)""")
    abstract suspend fun deleteUserChatLinks(channelId: Long, vararg userIds: String)

    @Query("""DELETE FROM $USER_CHAT_LINK_TABLE WHERE chat_id = :channelId""")
    abstract suspend fun deleteChatLinks(channelId: Long)

    @Query("""DELETE FROM $USER_CHAT_LINK_TABLE WHERE chat_id IN (:channelIds)""")
    abstract suspend fun deleteChannelsLinks(channelIds: List<Long>)

    @Query("""DELETE FROM $USER_CHAT_LINK_TABLE WHERE chat_id = :channelId AND user_id != :exceptUserId""")
    abstract suspend fun deleteChatLinksExceptUser(channelId: Long, exceptUserId: String)

    @Transaction
    open suspend fun deleteChannelAndLinks(channelId: Long) {
        deleteChannel(channelId)
        deleteChatLinks(channelId)
    }

    @Query("""DELETE FROM $CHANNEL_TABLE WHERE chat_id IN (:ids)""")
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
