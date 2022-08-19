package com.sceyt.chat.ui.persistence.dao

import androidx.room.*
import com.sceyt.chat.ui.data.models.channels.RoleTypeEnum
import com.sceyt.chat.ui.persistence.entity.ChanelMember
import com.sceyt.chat.ui.persistence.entity.channel.ChannelDb
import com.sceyt.chat.ui.persistence.entity.channel.ChannelEntity
import com.sceyt.chat.ui.persistence.entity.channel.UserChatLink

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannel(channel: ChannelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannels(channel: List<ChannelEntity>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannelsAndLinks(channels: List<ChannelEntity>, userChatLinks: List<UserChatLink>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChannelAndLinks(channels: ChannelEntity, userChatLinks: List<UserChatLink>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserChatLinks(userChatLinks: List<UserChatLink>): List<Long>

    @Transaction
    @Query("select * from channels where myRole is null or myRole !=:ignoreRole " +
            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit :limit offset :offset")
    fun getChannels(limit: Int, offset: Int, ignoreRole: RoleTypeEnum = RoleTypeEnum.None): List<ChannelDb>

    @Transaction
    @Query("select * from channels where subject LIKE '%' || :query || '%' " +
            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit :limit offset :offset")
    fun getChannelsByQuery(limit: Int, offset: Int, query: String): List<ChannelDb>

    @Transaction
    @Query("select * from channels where chat_id =:id")
    fun getChannelById(id: Long): ChannelDb?

    @Query("select user_id from UserChatLink where chat_id =:channelId and role =:role")
    fun getChannelOwner(channelId: Long, role: String = RoleTypeEnum.Owner.toString()): String?

    @Transaction
    @Query("select * from UserChatLink join users on UserChatLink.user_id = users.user_id where chat_id =:channelId " +
            "order by user_id limit :limit offset :offset")
    fun getChannelMembers(channelId: Long, limit: Int, offset: Int): List<ChanelMember>

    @Query("update channels set subject =:subject, avatarUrl =:avatarUrl where chat_id= :channelId")
    fun updateChannelSubjectAndAvatarUrl(channelId: Long, subject: String, avatarUrl: String?)

    @Query("update channels set lastMessageId =:lastMessageId, lastMessageAt =:lastMessageAt where chat_id= :channelId")
    fun updateLastMessage(channelId: Long, lastMessageId: Long?, lastMessageAt: Long?)

    @Query("update channels set muted =:muted, muteExpireDate =:muteUntil where chat_id =:channelId")
    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0)

    @Query("update UserChatLink set role =:role where chat_id =:channelId and user_id =:userId")
    fun updateMemberRole(channelId: Long, userId: String, role: String)

    @Transaction
    fun updateOwner(channelId: Long, oldOwnerId: String, newOwnerId: String) {
        updateMemberRole(channelId, oldOwnerId, RoleTypeEnum.Member.toString())
        updateMemberRole(channelId, newOwnerId, RoleTypeEnum.Owner.toString())
    }

    @Transaction
    fun updateOwner(channelId: Long, newOwnerId: String) {
        getChannelOwner(channelId)?.let {
            updateMemberRole(channelId, it, RoleTypeEnum.Member.toString())
        }
        updateMemberRole(channelId, newOwnerId, RoleTypeEnum.Owner.toString())
    }

    @Query("delete from channels where chat_id =:channelId")
    fun deleteChannel(channelId: Long)

    @Query("delete from UserChatLink where chat_id =:channelId and user_id in (:userIds)")
    fun deleteUserChatLinks(channelId: Long, vararg userIds: String)

    @Query("delete from UserChatLink where chat_id =:channelId")
    fun deleteChatLinks(channelId: Long)

    @Transaction
    fun deleteChannelAndLinks(channelId: Long) {
        deleteChannel(channelId)
        deleteChatLinks(channelId)
    }
}