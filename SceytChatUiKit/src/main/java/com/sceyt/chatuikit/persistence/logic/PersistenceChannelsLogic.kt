package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.chatuikit.data.channeleventobserver.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.chatuikit.pushes.RemoteMessageData
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.flow.Flow

interface PersistenceChannelsLogic {
    suspend fun onChannelEvent(data: ChannelEventData)
    suspend fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData)
    suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData)
    suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>)
    suspend fun onFcmMessage(data: RemoteMessageData)
    suspend fun onMessageEditedOrDeleted(message: SceytMessage)
    suspend fun loadChannels(offset: Int, searchQuery: String,
                             loadKey: LoadKeyData?, ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>>

    suspend fun searchChannelsWithUserIds(offset: Int, limit: Int, searchQuery: String,
                                          userIds: List<String>, includeUserNames: Boolean,
                                          loadKey: LoadKeyData?, onlyMine: Boolean, ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>>

    suspend fun syncChannels(limit: Int): Flow<GetAllChannelsResponse>
    suspend fun findOrCreateDirectChannel(user: User): SceytResponse<SceytChannel>
    suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel>
    suspend fun createNewChannelInsteadOfPendingChannel(channel: SceytChannel): SceytResponse<SceytChannel>
    suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel>
    suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long>
    suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun leaveChannel(channelId: Long): SceytResponse<Long>
    suspend fun deleteChannel(channelId: Long): SceytResponse<Long>
    suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel>
    suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun enableAutoDelete(channelId: Long, period: Long): SceytResponse<SceytChannel>
    suspend fun disableAutoDelete(channelId: Long): SceytResponse<SceytChannel>
    suspend fun pinChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun unpinChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromDb(channelId: Long): SceytChannel?
    suspend fun getDirectChannelFromDb(peerId: String): SceytChannel?
    suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel>
    suspend fun getChannelFromServerByUrl(url: String): SceytResponse<List<SceytChannel>>
    suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel>
    suspend fun join(channelId: Long): SceytResponse<SceytChannel>
    suspend fun setUnreadCount(channelId: Long, count: Int)
    suspend fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage)
    suspend fun blockUnBlockUser(userId: String, block: Boolean)
    suspend fun updateDraftMessage(channelId: Long, message: String?, mentionUsers: List<Mention>,
                                   styling: List<BodyStyleRange>?, replyOrEditMessage: SceytMessage?, isReply: Boolean)

    suspend fun getChannelsCountFromDb(): Int
    fun getTotalUnreadCount(): Flow<Int>
    suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>)
}