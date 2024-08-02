package com.sceyt.chatuikit.persistence.logicimpl.channelslogic

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.copy
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelsComparatorDescBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class ChannelsCache {
    private var cachedData = hashMapOf<Long, SceytChannel>()
    private var pendingChannelsData = hashMapOf<Long, SceytChannel>()

    internal val initialized: Boolean
        get() = cachedData.isNotEmpty()

    /** fromPendingToRealChannelsData is used to store created pending channel ids and their real channel ids,
     * to escape creating channel every time when sending message*/
    private var fromPendingToRealChannelsData = hashMapOf<Long, Long>()
    private val lock = Any()

    companion object {
        private val channelUpdatedFlow_ = MutableSharedFlow<ChannelUpdateData>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelUpdatedFlow: SharedFlow<ChannelUpdateData> = channelUpdatedFlow_

        private val channelReactionMsgLoadedFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelReactionMsgLoadedFlow: SharedFlow<SceytChannel> = channelReactionMsgLoadedFlow_

        private val channelDeletedFlow_ = MutableSharedFlow<Long>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelDeletedFlow: SharedFlow<Long> = channelDeletedFlow_

        private val channelAddedFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelAddedFlow: SharedFlow<SceytChannel> = channelAddedFlow_

        private val pendingChannelCreatedFlow_ = MutableSharedFlow<Pair<Long, SceytChannel>>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val pendingChannelCreatedFlow: SharedFlow<Pair<Long, SceytChannel>> = pendingChannelCreatedFlow_

        private val channelDraftMessageChangesFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelDraftMessageChangesFlow: SharedFlow<SceytChannel> = channelDraftMessageChangesFlow_

        var currentChannelId: Long? = null
    }

    /** Added channels like upsert, and check is differences between channels*/
    fun addAll(list: List<SceytChannel>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(list)
            else {
                putToCache(*list.toTypedArray())
                false
            }
        }
    }

    fun add(channel: SceytChannel) {
        synchronized(lock) {
            if (putAndCheckHasDiff(channel).hasDifference())
                channelAdded(channel)
        }
    }

    fun addPendingChannel(channel: SceytChannel) {
        synchronized(lock) {
            pendingChannelsData[channel.id] = channel
            if (channel.lastMessage != null)
                channelAdded(channel)
        }
    }

    fun clear() {
        synchronized(lock) {
            cachedData.clear()
        }
    }

    fun getSorted(): List<SceytChannel> {
        synchronized(lock) {
            return cachedData.values.sortedWith(ChannelsComparatorDescBy())
        }
    }

    fun getData(): List<SceytChannel> {
        synchronized(lock) {
            return cachedData.values.toList()
        }
    }

    fun get(channelId: Long): SceytChannel? {
        synchronized(lock) {
            return cachedData[channelId] ?: pendingChannelsData[channelId]
        }
    }

    fun getRealChannelIdWithPendingChannelId(pendingChannelId: Long): Long? {
        synchronized(lock) {
            return fromPendingToRealChannelsData[pendingChannelId]
        }
    }

    fun upsertChannel(vararg channels: SceytChannel) {
        synchronized(lock) {
            channels.forEach {
                val cachedChannel = cachedData[it.id] ?: pendingChannelsData[it.id]
                if (cachedChannel == null) {
                    if (!it.pending) {
                        cachedData[it.id] = it
                        channelAdded(it)
                    }
                } else {
                    val oldMsg = cachedChannel.lastMessage
                    val diff = putAndCheckHasDiff(it)
                    if (diff.hasDifference()) {
                        val needSort = checkNeedSortByLastMessage(oldMsg, it.lastMessage) || diff.pinStateChanged
                        channelUpdated(it, needSort, ChannelUpdatedType.Updated)
                    }
                }
            }
        }
    }

    fun updateLastMessage(channelId: Long, message: SceytMessage?) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                val updatedChannel = channel.copy(lastMessage = message)
                cachedData[channelId] = updatedChannel
                channelUpdated(updatedChannel, needSort, ChannelUpdatedType.LastMessage)
            }
        }
    }

    fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                val updatedChannel = channel.copy(
                    lastMessage = message,
                    lastDisplayedMessageId = message.id)
                cachedData[channelId] = updatedChannel
                channelUpdated(updatedChannel, needSort, ChannelUpdatedType.LastMessage)
            }
        }
    }

    fun clearedHistory(channelId: Long) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val updatedChannel = channel.copy(
                    lastMessage = null,
                    newMessageCount = 0,
                    newMentionCount = 0,
                    newReactedMessageCount = 0,
                    newReactions = null,
                    pendingReactions = null
                )
                cachedData[channelId] = updatedChannel
                channelUpdated(updatedChannel, true, ChannelUpdatedType.ClearedHistory)
            }
        }
    }

    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long = 0) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val updatedChannel = channel.copy(
                    muted = muted,
                    mutedTill = if (muted) muteUntil else 0
                )
                cachedData[channelId] = updatedChannel
                channelUpdated(updatedChannel, false, ChannelUpdatedType.MuteState)
            }
        }
    }

    fun updatePinState(channelId: Long, pinnedAt: Long?) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val updatedChannel = channel.copy(pinnedAt = pinnedAt)
                cachedData[channelId] = updatedChannel
                channelUpdated(updatedChannel, true, ChannelUpdatedType.PinnedAt)
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val updatedChannel = channel.copy(newMessageCount = count.toLong(), unread = false)
                cachedData[channelId] = updatedChannel
                channelUpdated(updatedChannel, false, ChannelUpdatedType.UnreadCount)
            }
        }
    }

    fun deleteChannel(id: Long) {
        synchronized(lock) {
            cachedData.remove(id)
            channelDeletedFlow_.tryEmit(id)
        }
    }

    fun removeFromPendingToRealChannelsData(pendingChannelId: Long) {
        synchronized(lock) {
            fromPendingToRealChannelsData.remove(pendingChannelId)
        }
    }

    fun pendingChannelCreated(pendingChannelId: Long, newChannel: SceytChannel) {
        synchronized(lock) {
            // Removing pending channel
            pendingChannelsData.remove(pendingChannelId)
            // Adding already created channel to cache
            cachedData[newChannel.id] = newChannel
            // Adding pending channel id with real channel id for future getting real channel id by pending channel id
            fromPendingToRealChannelsData[pendingChannelId] = newChannel.id
            // Emitting to flow
            pendingChannelCreatedFlow_.tryEmit(Pair(pendingChannelId, newChannel))
        }
    }

    private fun channelUpdated(channel: SceytChannel, needSort: Boolean, type: ChannelUpdatedType) {
        channelUpdatedFlow_.tryEmit(ChannelUpdateData(channel, needSort, type))
    }

    private fun channelAdded(channel: SceytChannel) {
        channelAddedFlow_.tryEmit(channel)
    }

    fun updateMembersCount(channel: SceytChannel) {
        var found = false
        synchronized(lock) {
            cachedData[channel.id]?.let {
                val updatedChannel = it.copy(memberCount = channel.memberCount)
                cachedData[channel.id] = updatedChannel
                channelUpdated(updatedChannel, false, ChannelUpdatedType.Members)
                found = true
            }
        }
        if (!found)
            upsertChannel(channel)
    }

    fun updateChannelDraftMessage(channelId: Long, draftMessage: DraftMessage?) {
        synchronized(lock) {
            cachedData[channelId]?.let {
                val updatedChannel = it.copy(draftMessage = draftMessage)
                cachedData[channelId] = updatedChannel
                channelDraftMessageChangesFlow_.tryEmit(updatedChannel)
            }
        }
    }

    fun updateChannelPeer(id: Long, user: User) {
        synchronized(lock) {
            cachedData[id]?.let { channel ->
                channel.members?.find { member -> member.user.id == user.id }?.let {
                    val oldUser = it.user
                    if (oldUser.diff(user).hasDifference()) {
                        it.user = user.copy()
                        channelUpdated(channel, false, ChannelUpdatedType.Presence)
                    }
                }
            }
        }
    }

    fun removeChannelMessageReactions(channelId: Long, messageId: Long) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val filteredReactions = channel.newReactions?.filter { it.messageId != messageId }
                val updatedChannel = channel.copy(newReactions = filteredReactions)
                cachedData[channelId] = updatedChannel
            }
        }
    }

    fun channelLastReactionLoaded(channelId: Long) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channelReactionMsgLoadedFlow_.tryEmit(channel)
            }
        }
    }

    fun onChannelMarkedAsReadOrUnread(channel: SceytChannel) {
        synchronized(lock) {
            cachedData[channel.id]?.let {
                val updatedChannel = it.copy(
                    unread = channel.unread,
                    newMessageCount = channel.newMessageCount
                )
                cachedData[channel.id] = updatedChannel
                channelUpdated(updatedChannel, false, ChannelUpdatedType.Updated)
            }
        }
    }

    private fun putAndCheckHasDiff(list: List<SceytChannel>): Boolean {
        var detectedDiff = false
        list.forEach {
            if (!detectedDiff) {
                val old = cachedData[it.id]
                detectedDiff = old?.diff(it)?.hasDifference() ?: true
            }
            putToCache(it)
        }
        return detectedDiff
    }

    private fun putAndCheckHasDiff(channel: SceytChannel): ChannelDiff {
        val old = cachedData[channel.id]
        cachedData[channel.id] = channel
        return old?.diff(channel) ?: ChannelDiff.DEFAULT
    }

    private fun putToCache(vararg channel: SceytChannel) {
        channel.groupBy { it.pending }.forEach { group ->
            if (group.key)
                pendingChannelsData.putAll(group.value.associateBy { channel -> channel.id })
            else
                cachedData.putAll(group.value.associateBy { channel -> channel.id })
        }
    }

    private fun checkNeedSortByLastMessage(oldMsg: SceytMessage?, newMsg: SceytMessage?): Boolean {
        return oldMsg?.id != newMsg?.id || oldMsg?.createdAt != newMsg?.createdAt
    }
}