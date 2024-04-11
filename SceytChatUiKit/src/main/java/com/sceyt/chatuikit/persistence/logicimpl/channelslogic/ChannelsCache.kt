package com.sceyt.chatuikit.persistence.logicimpl.channelslogic

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.copy
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.extensions.toArrayList
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
            pendingChannelsData[channel.id] = channel.clone()
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
            return cachedData.values.sortedWith(ChannelsComparatorDescBy()).map { it.clone() }
        }
    }

    fun getData(): List<SceytChannel> {
        synchronized(lock) {
            return cachedData.values.map { it.clone() }
        }
    }

    fun get(channelId: Long): SceytChannel? {
        synchronized(lock) {
            return cachedData[channelId]?.clone() ?: pendingChannelsData[channelId]?.clone()
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
                        cachedData[it.id] = it.clone()
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
                channel.lastMessage = message?.clone()
                channelUpdated(channel, needSort, ChannelUpdatedType.LastMessage)
            }
        }
    }

    fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                channel.lastMessage = message.clone()
                channel.lastDisplayedMessageId = message.id
                channelUpdated(channel, needSort, ChannelUpdatedType.LastMessage)
            }
        }
    }

    fun clearedHistory(channelId: Long) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channel.lastMessage = null
                channel.newMessageCount = 0
                channel.newMentionCount = 0
                channel.newReactedMessageCount = 0
                channel.newReactions = null
                channel.pendingReactions = null
                channelUpdated(channel, true, ChannelUpdatedType.ClearedHistory)
            }
        }
    }

    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long = 0) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                if (muted) {
                    channel.muted = true
                    channel.mutedTill = muteUntil
                } else channel.muted = false

                channelUpdated(channel, false, ChannelUpdatedType.MuteState)
            }
        }
    }

    fun updatePinState(channelId: Long, pinnedAt: Long?) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channel.pinnedAt = pinnedAt
                channelUpdated(channel, true, ChannelUpdatedType.PinnedAt)
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channel.newMessageCount = count.toLong()
                channel.unread = false
                channelUpdated(channel, false, ChannelUpdatedType.UnreadCount)
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
            cachedData[newChannel.id] = newChannel.clone()
            // Adding pending channel id with real channel id for future getting real channel id by pending channel id
            fromPendingToRealChannelsData[pendingChannelId] = newChannel.id
            // Emitting to flow
            pendingChannelCreatedFlow_.tryEmit(Pair(pendingChannelId, newChannel.clone()))
        }
    }

    private fun channelUpdated(channel: SceytChannel, needSort: Boolean, type: ChannelUpdatedType) {
        channelUpdatedFlow_.tryEmit(ChannelUpdateData(channel.clone(), needSort, type))
    }

    private fun channelAdded(channel: SceytChannel) {
        channelAddedFlow_.tryEmit(channel.clone())
    }

    fun updateMembersCount(channel: SceytChannel) {
        var found = false
        synchronized(lock) {
            cachedData[channel.id]?.let {
                it.memberCount = channel.memberCount
                channelUpdated(it, false, ChannelUpdatedType.Members)
                found = true
            }
        }
        if (!found)
            upsertChannel(channel)
    }

    fun updateChannelDraftMessage(channelId: Long, draftMessage: DraftMessage?) {
        synchronized(lock) {
            cachedData[channelId]?.let {
                it.draftMessage = draftMessage?.copy()
                channelDraftMessageChangesFlow_.tryEmit(it.clone())
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
                channel.newReactions?.filter { it.messageId == messageId }?.let {
                    channel.newReactions = channel.newReactions?.toArrayList()?.apply {
                        removeAll(it.toSet())
                    }
                }
            }
        }
    }

    fun channelLastReactionLoaded(channelId: Long) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channelReactionMsgLoadedFlow_.tryEmit(channel.clone())
            }
        }
    }

    fun onChannelMarkedAsReadOrUnread(channel: SceytChannel) {
        synchronized(lock) {
            cachedData[channel.id]?.let {
                it.unread = channel.unread
                it.newMessageCount = channel.newMessageCount
                channelUpdated(it, false, ChannelUpdatedType.Updated)
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
        cachedData[channel.id] = channel.clone()
        return old?.diff(channel) ?: ChannelDiff.DEFAULT
    }

    private fun putToCache(vararg channel: SceytChannel) {
        channel.groupBy { it.pending }.forEach { group ->
            if (group.key)
                pendingChannelsData.putAll(group.value.map { it.clone() }.associateBy { channel -> channel.id })
            else
                cachedData.putAll(group.value.map { it.clone() }.associateBy { channel -> channel.id })
        }
    }

    private fun checkNeedSortByLastMessage(oldMsg: SceytMessage?, newMsg: SceytMessage?): Boolean {
        return oldMsg?.id != newMsg?.id || oldMsg?.createdAt != newMsg?.createdAt
    }
}