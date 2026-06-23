package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.LocalUnreadCountsDb
import com.sceyt.chatuikit.persistence.database.dao.LocalUnreadDao
import com.sceyt.chatuikit.persistence.database.entity.channel.LocalChannelUnreadStateEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.LocalUnreadMessageEntity
import com.sceyt.chatuikit.persistence.mappers.toChannel
import kotlin.math.max

internal class LocalUnreadCountsManager(
    private val localUnreadDao: LocalUnreadDao,
    private val channelDao: ChannelDao,
    private val channelsCache: ChannelsCache,
) {

    suspend fun seedChannel(channel: SceytChannel): SceytChannel {
        return seedChannels(listOf(channel)).first()
    }

    suspend fun seedChannels(channels: List<SceytChannel>): List<SceytChannel> {
        if (channels.isEmpty()) return emptyList()

        val channelIds = channels.map { it.id }
        val stateMap = localUnreadDao.getStates(channelIds).associateBy { it.channelId }
        val mergedStates = stateMap.toMutableMap()
        val now = System.currentTimeMillis()

        val statesToUpsert = channels.mapNotNull { channel ->
            val current = stateMap[channel.id]
            if (current?.locallyManaged == true) return@mapNotNull null

            val baseUnreadCount = channel.newMessageCount.coerceAtLeast(0)
            val baseMentionCount = channel.newMentionCount.coerceAtLeast(0)
            if (current == null && baseUnreadCount == 0L && baseMentionCount == 0L && !channel.unread)
                return@mapNotNull null

            LocalChannelUnreadStateEntity(
                channelId = channel.id,
                baseUnreadCount = baseUnreadCount,
                baseMentionCount = baseMentionCount,
                baseUntilMessageId = channel.latestKnownMessageId(),
                lastLocalReadMessageId = current?.lastLocalReadMessageId ?: 0,
                markedUnread = channel.unread && baseUnreadCount == 0L,
                locallyManaged = false,
                updatedAt = now
            ).also { mergedStates[channel.id] = it }
        }

        if (statesToUpsert.isNotEmpty())
            localUnreadDao.upsertStates(statesToUpsert)

        val countsMap = localUnreadDao.getObservedCounts(channelIds).associateBy { it.channelId }
        return channels.map { channel ->
            channel.withLocalUnreadState(
                state = mergedStates[channel.id],
                counts = countsMap[channel.id]
            )
        }
    }

    suspend fun applyLocalState(channel: SceytChannel): SceytChannel {
        return applyLocalState(listOf(channel)).first()
    }

    suspend fun applyLocalState(channels: List<SceytChannel>): List<SceytChannel> {
        if (channels.isEmpty()) return emptyList()
        val channelIds = channels.map { it.id }
        val stateMap = localUnreadDao.getStates(channelIds).associateBy { it.channelId }
        val countsMap = localUnreadDao.getObservedCounts(channelIds).associateBy { it.channelId }
        return channels.map { channel ->
            channel.withLocalUnreadState(
                state = stateMap[channel.id],
                counts = countsMap[channel.id]
            )
        }
    }

    suspend fun recordObservedMessages(messages: List<SceytMessage>) {
        val candidates = messages.filter {
            it.id > 0 && it.incoming && ChannelsCache.currentChannelId != it.channelId
        }
        if (candidates.isEmpty()) return

        val channelIds = candidates.map { it.channelId }.distinct()
        val existingStates = localUnreadDao.getStates(channelIds).associateBy { it.channelId }
        val now = System.currentTimeMillis()
        val states = existingStates.toMutableMap()
        val missingStates = channelIds.mapNotNull { channelId ->
            if (states.containsKey(channelId)) return@mapNotNull null
            LocalChannelUnreadStateEntity(
                channelId = channelId,
                baseUnreadCount = 0,
                baseMentionCount = 0,
                baseUntilMessageId = 0,
                lastLocalReadMessageId = 0,
                markedUnread = false,
                locallyManaged = false,
                updatedAt = now
            ).also { states[channelId] = it }
        }
        if (missingStates.isNotEmpty())
            localUnreadDao.upsertStates(missingStates)

        val unreadRows = candidates.mapNotNull { message ->
            val state = states[message.channelId] ?: return@mapNotNull null
            if (message.id <= max(state.baseUntilMessageId, state.lastLocalReadMessageId))
                return@mapNotNull null
            LocalUnreadMessageEntity(
                channelId = message.channelId,
                messageId = message.id,
                mentioned = message.isMentioningCurrentUser(),
                createdAt = message.createdAt
            )
        }
        if (localUnreadDao.insertUnreadMessages(unreadRows))
            refreshStoredChannels(unreadRows.map { it.channelId }.distinct())
    }

    suspend fun markRead(channel: SceytChannel): SceytChannel {
        val latestMessageId = channel.latestKnownMessageId()
        localUnreadDao.deleteUnreadMessages(channel.id)
        localUnreadDao.upsertState(
            LocalChannelUnreadStateEntity(
                channelId = channel.id,
                baseUnreadCount = 0,
                baseMentionCount = 0,
                baseUntilMessageId = latestMessageId,
                lastLocalReadMessageId = latestMessageId,
                markedUnread = false,
                locallyManaged = true,
                updatedAt = System.currentTimeMillis()
            )
        )
        return applyLocalState(channel.copy(lastDisplayedMessageId = latestMessageId))
            .also { updateStoredChannelUnreadState(it) }
    }

    suspend fun markUnread(channel: SceytChannel): SceytChannel {
        val state = localUnreadDao.getState(channel.id)
        localUnreadDao.upsertState(
            LocalChannelUnreadStateEntity(
                channelId = channel.id,
                baseUnreadCount = state?.baseUnreadCount ?: 0,
                baseMentionCount = state?.baseMentionCount ?: 0,
                baseUntilMessageId = state?.baseUntilMessageId ?: channel.latestKnownMessageId(),
                lastLocalReadMessageId = state?.lastLocalReadMessageId ?: 0,
                markedUnread = true,
                locallyManaged = true,
                updatedAt = System.currentTimeMillis()
            )
        )
        return applyLocalState(channel).also { updateStoredChannelUnreadState(it) }
    }

    suspend fun markMessagesRead(channelId: Long, messageIds: List<Long>) {
        if (messageIds.isEmpty()) return
        val latestReadMessageId = messageIds.maxOrNull() ?: 0
        val state = localUnreadDao.getState(channelId)
        if (latestReadMessageId > (state?.lastLocalReadMessageId ?: 0)) {
            localUnreadDao.upsertState(
                LocalChannelUnreadStateEntity(
                    channelId = channelId,
                    baseUnreadCount = state?.baseUnreadCount ?: 0,
                    baseMentionCount = state?.baseMentionCount ?: 0,
                    baseUntilMessageId = max(state?.baseUntilMessageId ?: 0, latestReadMessageId),
                    lastLocalReadMessageId = latestReadMessageId,
                    markedUnread = false,
                    locallyManaged = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        localUnreadDao.deleteUnreadMessages(channelId, messageIds)
        refreshStoredChannels(listOf(channelId))
    }

    suspend fun clearChannel(channelId: Long) {
        localUnreadDao.deleteUnreadMessages(channelId)
        localUnreadDao.deleteState(channelId)
        refreshStoredChannels(listOf(channelId))
    }

    suspend fun clearChannels(channelIds: List<Long>) {
        if (channelIds.isEmpty()) return
        localUnreadDao.deleteUnreadMessages(channelIds)
        localUnreadDao.deleteStates(channelIds)
    }

    private suspend fun refreshStoredChannels(channelIds: List<Long>) {
        if (channelIds.isEmpty()) return
        val channels = channelDao.getChannelsById(channelIds).map { it.toChannel() }
        applyLocalState(channels).forEach { updateStoredChannelUnreadState(it) }
    }

    private suspend fun updateStoredChannelUnreadState(channel: SceytChannel) {
        channelDao.updateUnreadState(
            channelId = channel.id,
            unread = channel.unread,
            newMessageCount = channel.newMessageCount,
            newMentionCount = channel.newMentionCount,
            lastDisplayedMessageId = channel.lastDisplayedMessageId
        )
        channelsCache.updateLocalUnreadState(
            channelId = channel.id,
            unread = channel.unread,
            newMessageCount = channel.newMessageCount,
            newMentionCount = channel.newMentionCount,
            lastDisplayedMessageId = channel.lastDisplayedMessageId
        )
    }

    private fun SceytChannel.withLocalUnreadState(
        state: LocalChannelUnreadStateEntity?,
        counts: LocalUnreadCountsDb?,
    ): SceytChannel {
        val unreadCount = (state?.baseUnreadCount ?: 0) + (counts?.unreadCount ?: 0)
        val mentionCount = (state?.baseMentionCount ?: 0) + (counts?.mentionCount ?: 0)
        return copy(
            unread = unreadCount > 0 || state?.markedUnread == true,
            newMessageCount = unreadCount,
            newMentionCount = mentionCount,
            lastDisplayedMessageId = max(lastDisplayedMessageId, state?.lastLocalReadMessageId ?: 0)
        )
    }

    private fun SceytChannel.latestKnownMessageId(): Long {
        return max(max(lastMessage?.id ?: 0, lastDisplayedMessageId), lastReceivedMessageId)
    }

    private fun SceytMessage.isMentioningCurrentUser(): Boolean {
        return !disableMentionsCount
                && displayCount.toInt() > 0
                && mentionedUsers.orEmpty().any { it.id == SceytChatUIKit.currentUserId }
    }
}
