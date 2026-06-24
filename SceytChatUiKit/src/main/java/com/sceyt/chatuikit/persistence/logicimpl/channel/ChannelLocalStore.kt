package com.sceyt.chatuikit.persistence.logicimpl.channel

import androidx.sqlite.db.SimpleSQLiteQuery
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.DraftMessageDao
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toDraftMessage
import kotlinx.coroutines.flow.Flow

internal class ChannelLocalStore(
    private val channelDao: ChannelDao,
    private val draftMessageDao: DraftMessageDao,
    private val channelsCache: ChannelsCache,
    private val overlayResolver: ChannelLocalOverlayResolver,
    private val localUnreadCountsManager: LocalUnreadCountsManager,
) {

    suspend fun applyServerState(channel: SceytChannel): SceytChannel {
        return overlayResolver.fromServer(channel)
    }

    suspend fun applyServerState(channels: List<SceytChannel>): List<SceytChannel> {
        return overlayResolver.fromServer(channels)
    }

    suspend fun applyStoredState(channel: SceytChannel): SceytChannel {
        return overlayResolver.fromStore(channel)
    }

    suspend fun applyStoredState(channels: List<SceytChannel>): List<SceytChannel> {
        return overlayResolver.fromStore(channels)
    }

    suspend fun getChannel(channelId: Long): SceytChannel? {
        return channelDao.getChannelById(channelId)?.toChannel()
            ?.withDraftState()
            ?.let { applyStoredState(it) }
    }

    suspend fun getChannels(channelIds: List<Long>): List<SceytChannel> {
        return applyStoredState(
            channelDao.getChannelsById(channelIds).map { it.toChannel().withDraftState() })
    }

    suspend fun getRawChannel(channelId: Long): SceytChannel? {
        return channelDao.getChannelById(channelId)?.toChannel()?.withDraftState()
    }

    suspend fun getRawChannelByUri(uri: String): SceytChannel? {
        return channelDao.getChannelByUri(uri)?.toChannel()?.withDraftState()
    }

    suspend fun getRawChannelsByPeerId(peerId: String): List<SceytChannel> {
        return channelDao.getChannelByPeerId(peerId).map { it.toChannel().withDraftState() }
    }

    suspend fun getDirectChannel(peerId: String): SceytChannel? {
        return channelDao.getChannelByUserAndType(peerId, ChannelTypeEnum.Direct.value)?.toChannel()
            ?.withDraftState()
            ?.let { applyStoredState(it) }
    }

    suspend fun getChannels(
        limit: Int,
        offset: Int,
        types: List<String>,
        orderByLastMessage: Boolean,
        onlyMine: Boolean,
    ): List<SceytChannel> {
        return applyStoredState(
            channelDao.getChannels(
                limit = limit,
                offset = offset,
                types = types,
                orderByLastMessage = orderByLastMessage,
                onlyMine = onlyMine
            ).map { it.toChannel().withDraftState() }
        )
    }

    suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<SceytChannel> {
        return applyStoredState(
            channelDao.getChannelsBySQLiteQuery(query).map { it.toChannel().withDraftState() })
    }

    suspend fun getCachedOrStoredChannel(channelId: Long): SceytChannel? {
        return channelsCache.getOneOf(channelId) ?: getChannel(channelId)
    }

    suspend fun upsertCacheWithStoredState(channel: SceytChannel) {
        channelsCache.upsertChannel(applyStoredState(channel))
    }

    suspend fun upsertCacheWithStoredState(channels: List<SceytChannel>) {
        channelsCache.upsertChannels(applyStoredState(channels))
    }

    suspend fun getAndUpsertCache(channelId: Long): SceytChannel? {
        return getChannel(channelId)?.also { channelsCache.upsertChannel(it) }
    }

    suspend fun getNotExistingChannelIdsByIdsAndTypes(
        ids: List<Long>,
        types: List<String>,
        onlyMine: Boolean,
    ): List<Long> {
        return channelDao.getNotExistingChannelIdsByIdsAndTypes(ids, types, onlyMine)
    }

    suspend fun getAllChannelIdsByTypes(types: List<String>, onlyMine: Boolean): List<Long> {
        return channelDao.getAllChannelIdsByTypes(types, onlyMine)
    }

    suspend fun getAllChannelsCount(): Int {
        return channelDao.getAllChannelsCount()
    }

    suspend fun getRetentionPeriod(channelId: Long): Long? {
        return channelDao.getRetentionPeriodByChannelId(channelId)
    }

    suspend fun getChannelsLastMessageTIds(channelIds: List<Long>): List<Long> {
        return channelDao.getChannelsLastMessageTIds(channelIds)
    }

    suspend fun getChannelLastMessageTid(channelId: Long): Long? {
        return channelDao.getChannelLastMessageTid(channelId)
    }

    fun getTotalUnreadCountAsFlow(channelTypes: List<String>): Flow<Long> {
        return channelDao.getTotalUnreadCountAsFlow(channelTypes)
    }

    suspend fun insertChannelAndLinks(
        channel: SceytChannel,
        userChatLinks: List<UserChatLinkEntity>,
    ) {
        channelDao.insertChannelAndLinks(channel.toChannelEntity(), userChatLinks)
    }

    suspend fun insertChannelsAndLinks(
        channels: List<SceytChannel>,
        userChatLinks: List<UserChatLinkEntity>,
    ) {
        channelDao.insertChannelsAndLinks(channels.map { it.toChannelEntity() }, userChatLinks)
    }

    suspend fun updateChannel(channel: SceytChannel): Boolean {
        return channelDao.updateChannel(channel.toChannelEntity()) == 1
    }

    suspend fun updateLastMessage(
        channelId: Long,
        lastMessageTid: Long?,
        lastMessageAt: Long?,
    ) {
        channelDao.updateLastMessage(channelId, lastMessageTid, lastMessageAt)
    }

    suspend fun updateLastMessage(
        channelId: Long,
        message: SceytMessage?,
    ) {
        updateLastMessage(
            channelId = channelId,
            lastMessageTid = message?.lastMessageTid(),
            lastMessageAt = message?.createdAt
        )
        channelsCache.updateLastMessage(channelId, message)
    }

    suspend fun updateCachedLastMessage(
        channelId: Long,
        message: SceytMessage?,
    ) {
        channelsCache.updateLastMessage(channelId, message)
    }

    suspend fun updateLastMessageWithLastRead(
        channelId: Long,
        lastMessageTid: Long?,
        lastMessageId: Long,
        lastMessageAt: Long?,
    ) {
        channelDao.updateLastMessageWithLastRead(
            channelId = channelId,
            lastMessageTid = lastMessageTid,
            lastMessageId = lastMessageId,
            lastMessageAt = lastMessageAt
        )
    }

    suspend fun updateLastMessageWithLastRead(
        channelId: Long,
        message: SceytMessage?,
    ) {
        updateLastMessageWithLastRead(
            channelId = channelId,
            lastMessageTid = message?.lastMessageTid(),
            lastMessageId = message?.id ?: 0,
            lastMessageAt = message?.createdAt
        )
        channelsCache.updateLastMessageWithLastRead(channelId, message)
    }

    suspend fun updateLastMessageWithLastReadIfNeeded(
        channelId: Long,
        message: SceytMessage,
    ) {
        val cachedChannel = channelsCache.getOneOf(channelId)
        val channel = cachedChannel ?: getChannel(channelId) ?: return

        if (message.deliveryStatus == MessageDeliveryStatus.Pending) {
            updateLastMessage(
                channelId = channelId,
                lastMessageTid = message.lastMessageTid(),
                lastMessageAt = message.createdAt
            )
            if (cachedChannel == null) {
                channelsCache.upsertChannel(channel)
            } else {
                channelsCache.updateLastMessage(channelId, message)
            }
            return
        }

        if (channel.lastMessage?.tid != message.tid) return

        updateLastMessageWithLastRead(
            channelId = channelId,
            lastMessageTid = message.lastMessageTid(),
            lastMessageId = message.id,
            lastMessageAt = message.createdAt
        )

        if (cachedChannel != null) {
            channelsCache.updateLastMessageWithLastRead(channelId, message)
        } else {
            channelsCache.upsertChannel(channel)
        }
    }

    suspend fun updateLastMessageOnMessagesResponseIfNeeded(
        channelId: Long,
        message: SceytMessage?,
    ) {
        val cachedChannel = channelsCache.getOneOf(channelId)
        val needToUpdateLastMessage = message?.deliveryStatus != MessageDeliveryStatus.Pending
                && cachedChannel?.lastMessage?.deliveryStatus != MessageDeliveryStatus.Pending

        if (needToUpdateLastMessage)
            updateLastMessage(channelId, message)
    }

    suspend fun updateMemberCount(channelId: Long, count: Int) {
        channelDao.updateMemberCount(channelId, count)
        getAndUpsertCache(channelId)
    }

    suspend fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long? = 0) {
        channelDao.updateMuteState(channelId, muted, muteUntil)
        channelsCache.updateMuteState(channelId, muted, muteUntil ?: 0)
    }

    suspend fun updateAutoDeleteState(channelId: Long, period: Long) {
        channelDao.updateAutoDeleteState(channelId, period)
        channelsCache.updateAutoDeleteState(channelId, period)
    }

    suspend fun updatePinState(channel: SceytChannel?) {
        channel ?: return
        channelDao.updatePinState(channel.id, channel.pinnedAt)
        channelsCache.updatePinState(channel.id, channel.pinnedAt)
    }

    suspend fun updateUri(channelId: Long, uri: String?) {
        channelDao.updateUri(channelId, uri)
    }

    suspend fun updateChannelUri(channelId: Long, uri: String) {
        channelDao.updateChannel(
            getRawChannel(channelId)?.copy(uri = uri)?.toChannelEntity() ?: return
        )
        channelsCache.updateChannelUri(channelId, uri)
    }

    suspend fun deleteUserChatLinks(channelId: Long, vararg userIds: String) {
        channelDao.deleteUserChatLinks(channelId, *userIds)
    }

    suspend fun deleteChatLinksExceptUser(channelId: Long, exceptUserId: String) {
        channelDao.deleteChatLinksExceptUser(channelId, exceptUserId)
    }

    suspend fun deleteChannelRecordAndCache(channelId: Long) {
        channelDao.deleteChannel(channelId)
        channelsCache.deleteChannel(channelId)
    }

    suspend fun deleteChannelAndLinks(channelId: Long) {
        channelDao.deleteChannelAndLinks(channelId)
    }

    suspend fun deleteAllChannelsAndLinks(channelIds: List<Long>) {
        channelDao.deleteAllChannelsAndLinksById(channelIds)
    }

    suspend fun markRead(channel: SceytChannel): SceytChannel {
        return localUnreadCountsManager.markRead(channel)
    }

    suspend fun markUnread(channel: SceytChannel): SceytChannel {
        return localUnreadCountsManager.markUnread(channel)
    }

    suspend fun clearUnread(channelId: Long) {
        localUnreadCountsManager.clearChannel(channelId)
    }

    suspend fun clearUnread(channelIds: List<Long>) {
        localUnreadCountsManager.clearChannels(channelIds)
    }

    private suspend fun SceytChannel.withDraftState(): SceytChannel {
        if (draftMessage != null) return this
        val draft = draftMessageDao.getDraftByChannelId(id)?.toDraftMessage() ?: return this
        return copy(draftMessage = draft)
    }

    private fun SceytMessage.lastMessageTid(): Long {
        return if (incoming) id else tid
    }
}
