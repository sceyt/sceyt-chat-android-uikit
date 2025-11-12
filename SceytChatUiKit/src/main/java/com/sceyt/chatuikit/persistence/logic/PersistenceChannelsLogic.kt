package com.sceyt.chatuikit.persistence.logic

import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.Flow

interface PersistenceChannelsLogic {
    suspend fun onChannelEvent(event: ChannelActionEvent)
    suspend fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>)
    suspend fun handlePush(data: PushData)
    suspend fun onMessageEditedOrDeleted(message: SceytMessage)
    fun loadChannels(
        offset: Int,
        searchQuery: String,
        loadKey: LoadKeyData?,
        onlyMine: Boolean,
        ignoreDb: Boolean,
        awaitForConnection: Boolean,
        config: ChannelListConfig,
    ): Flow<PaginationResponse<SceytChannel>>

    suspend fun searchChannelsWithUserIds(
        offset: Int,
        searchQuery: String,
        userIds: List<String>,
        config: ChannelListConfig,
        includeSearchByUserDisplayName: Boolean,
        onlyMine: Boolean,
        ignoreDb: Boolean,
        loadKey: LoadKeyData?,
        directChatType: String,
    ): Flow<PaginationResponse<SceytChannel>>

    suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<SceytChannel>
    suspend fun syncChannels(config: ChannelListConfig): Flow<GetAllChannelsResponse>
    suspend fun findOrCreatePendingChannelByMembers(data: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun findOrCreatePendingChannelByUri(data: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun createNewChannelInsteadOfPendingChannel(channel: SceytChannel): SceytResponse<SceytChannel>
    suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long>
    suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun unblockChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun leaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun deleteChannel(channelId: Long): SceytResponse<Long>
    suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun enableAutoDelete(channelId: Long, period: Long): SceytResponse<SceytChannel>
    suspend fun disableAutoDelete(channelId: Long): SceytResponse<SceytChannel>
    suspend fun pinChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun unpinChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun unHideChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromDb(channelId: Long): SceytChannel?
    suspend fun getChannelsFromDb(channelIds: List<Long>): List<SceytChannel>
    suspend fun getRetentionPeriodByChannelId(channelId: Long): Long
    suspend fun getDirectChannelFromDb(peerId: String): SceytChannel?
    suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromServerByUri(uri: String): SceytResponse<SceytChannel?>
    suspend fun getChannelByInviteKey(inviteKey: String): SceytResponse<SceytChannel>
    suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel>
    suspend fun join(channelId: Long): SceytResponse<SceytChannel>
    suspend fun joinWithInviteKey(inviteKey: String): SceytResponse<SceytChannel>
    suspend fun setUnreadCount(channelId: Long, count: Int)
    suspend fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage)
    suspend fun updateLastMessageOnMessagesResponseIfNeeded(channelId: Long, message: SceytMessage?)
    suspend fun blockUnBlockUser(userId: String, block: Boolean)
    suspend fun sendChannelEvent(channelId: Long, event: String)
    suspend fun updateDraftMessage(draftMessage: DraftMessage)
    suspend fun getChannelsCountFromDb(): Int
    suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>)
    suspend fun checkChannelUrlUpdate(channelId: Long, oldKey: String, newKey: String)
    fun getChannelMessageCount(channelId: Long): Flow<Long>
    fun getTotalUnreadCount(channelTypes: List<String>): Flow<Long>
}