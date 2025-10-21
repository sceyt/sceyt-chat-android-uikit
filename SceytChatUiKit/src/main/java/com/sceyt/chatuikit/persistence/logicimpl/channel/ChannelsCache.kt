package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.forEachKeyValue
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsComparatorDescBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ChannelsCache {
    private var cachedData = hashMapOf<ChannelListConfig, HashMap<Long, SceytChannel>>()
    private var pendingChannelsData = hashMapOf<Long, SceytChannel>()

    /** fromPendingToRealChannelsData is used to store created pending channel ids and their real channel ids,
     * to escape creating channel every time when sending message*/
    private var fromPendingToRealChannelsData = hashMapOf<Long, Long>()
    private val lock = Any()

    companion object {
        private val channelUpdatedFlow_ = MutableSharedFlow<ChannelUpdateData>(
            extraBufferCapacity = 50,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelUpdatedFlow = channelUpdatedFlow_.asSharedFlow()

        private val channelReactionMsgLoadedFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelReactionMsgLoadedFlow = channelReactionMsgLoadedFlow_.asSharedFlow()

        private val channelsDeletedFlow_ = MutableSharedFlow<List<Long>>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelsDeletedFlow = channelsDeletedFlow_.asSharedFlow()

        private val channelAddedFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelAddedFlow = channelAddedFlow_.asSharedFlow()

        private val pendingChannelCreatedFlow_ = MutableSharedFlow<Pair<Long, SceytChannel>>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val pendingChannelCreatedFlow = pendingChannelCreatedFlow_.asSharedFlow()

        private val channelDraftMessageChangesFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelDraftMessageChangesFlow = channelDraftMessageChangesFlow_.asSharedFlow()

        private val newChannelsOnSync_ = MutableSharedFlow<Pair<ChannelListConfig, List<SceytChannel>>>(
            extraBufferCapacity = 50,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val newChannelsOnSync = newChannelsOnSync_.asSharedFlow()

        var currentChannelId: Long? = null
    }

    /** Added channels like upsert, and check is differences between channels*/
    fun addAll(config: ChannelListConfig, list: List<SceytChannel>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            // Create config map if not exists
            getOrCreateMap(config)
            return if (checkDifference)
                putAndCheckHasDiff(config, list)
            else {
                putToCache(config, *list.toTypedArray())
                false
            }
        }
    }

    fun addPendingChannel(channel: SceytChannel) {
        synchronized(lock) {
            pendingChannelsData[channel.id] = channel
            if (channel.lastMessage != null)
                channelAdded(channel)
        }
    }

    fun clear(config: ChannelListConfig) {
        synchronized(lock) {
            cachedData[config]?.clear()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            cachedData.clear()
        }
    }

    fun getSorted(config: ChannelListConfig): List<SceytChannel> {
        synchronized(lock) {
            return channelsByConfig(config).sortedWith(ChannelsComparatorDescBy(config.order))
        }
    }

    fun getData(config: ChannelListConfig): List<SceytChannel> {
        synchronized(lock) {
            return channelsByConfig(config)
        }
    }

    fun getCachedData(): HashMap<ChannelListConfig, HashMap<Long, SceytChannel>> {
        synchronized(lock) {
            return cachedData
        }
    }

    fun getOneOf(channelId: Long, config: ChannelListConfig? = null): SceytChannel? {
        synchronized(lock) {
            return getChannelImpl(channelId, config)
        }
    }

    fun isPending(channelId: Long): Boolean {
        synchronized(lock) {
            return pendingChannelsData.containsKey(channelId)
        }
    }

    fun getRealChannelIdWithPendingChannelId(pendingChannelId: Long): Long? {
        synchronized(lock) {
            return fromPendingToRealChannelsData[pendingChannelId]
        }
    }

    fun upsertChannel(channel: SceytChannel) {
        upsertChannels(listOf(channel))
    }

    fun upsertChannels(channels: List<SceytChannel>) {
        synchronized(lock) {
            channels.forEach { channel ->
                cachedData.keys
                    .filter { it.isValidForConfig(channel) }
                    .forEach { upsertChannelImpl(it, channel) }
            }
        }
    }

    fun newChannelsOnSync(config: ChannelListConfig, channels: List<SceytChannel>) {
        newChannelsOnSync_.tryEmit(Pair(config, channels))
    }

    fun updateChannel(config: ChannelListConfig, vararg channels: SceytChannel) {
        synchronized(lock) {
            updateChannelsImpl(config, *channels)
        }
    }

    private fun upsertChannelImpl(config: ChannelListConfig, vararg channels: SceytChannel) {
        val map = getOrCreateMap(config)
        channels.forEach { channel ->
            val cachedChannel = map[channel.id] ?: pendingChannelsData[channel.id]
            if (cachedChannel == null) {
                if (!channel.pending) {
                    map[channel.id] = channel
                    channelAdded(channel)
                }
            } else {
                checkMaybePendingChannelCreated(cachedChannel, channel)
                val oldMsg = cachedChannel.lastMessage
                val diff = putAndCheckHasDiff(config, channel)
                if (diff.hasDifference()) {
                    val needSort = checkNeedSortByLastMessage(oldMsg, channel.lastMessage) || diff.pinStateChanged
                    channelUpdated(config, channel, diff, needSort, ChannelUpdatedType.Updated)
                }
            }
        }
    }

    private fun updateChannelsImpl(config: ChannelListConfig, vararg channels: SceytChannel) {
        val map = getOrCreateMap(config)
        channels.forEach { channel ->
            val cachedChannel = map[channel.id]
                    ?: pendingChannelsData[channel.id] ?: return@forEach

            checkMaybePendingChannelCreated(cachedChannel, channel)
            val oldMsg = cachedChannel.lastMessage
            val diff = putAndCheckHasDiff(config, channel)
            if (diff.hasDifference()) {
                val needSort = checkNeedSortByLastMessage(oldMsg, channel.lastMessage) || diff.pinStateChanged
                channelUpdated(config, channel, diff, needSort, ChannelUpdatedType.Updated)
            }
        }
    }

    private fun checkMaybePendingChannelCreated(cachedChannel: SceytChannel, newChannel: SceytChannel) {
        if (!cachedChannel.pending || newChannel.pending) return
        pendingChannelCreated(cachedChannel.id, newChannel)
    }

    fun updateLastMessage(channelId: Long, message: SceytMessage?) {
        synchronized(lock) {
            cachedData.forEachKeyValue { config, map ->
                map[channelId]?.let { channel ->
                    if (message != null && channel.lastMessage != null)
                        if (!channel.lastMessage.diff(message).hasDifference())
                            return@forEachKeyValue

                    val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                    val updatedChannel = channel.copy(lastMessage = message)
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(config, updatedChannel, diff, needSort, ChannelUpdatedType.LastMessage)
                }
            }
        }
    }

    fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            cachedData.forEachKeyValue { config, map ->
                map[channelId]?.let { channel ->
                    val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                    val updatedChannel = channel.copy(
                        lastMessage = message,
                        lastDisplayedMessageId = message.id
                    )
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(config, updatedChannel, diff, needSort, ChannelUpdatedType.LastMessage)
                }
            }
        }
    }

    fun clearedHistory(channelId: Long) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    val updatedChannel = channel.copy(
                        lastMessage = null,
                        newMessageCount = 0,
                        newMentionCount = 0,
                        newReactedMessageCount = 0,
                        newReactions = null,
                        pendingReactions = null
                    )
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, true, ChannelUpdatedType.ClearedHistory)
                }
            }
        }
    }

    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long = 0) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    val updatedChannel = channel.copy(
                        muted = muted,
                        mutedTill = if (muted) muteUntil else 0
                    )
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, false, ChannelUpdatedType.MuteState)
                }
            }
        }
    }

    fun updateAutoDeleteState(channelId: Long, period: Long) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    val updatedChannel = channel.copy(messageRetentionPeriod = period)
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, false, ChannelUpdatedType.AutoDeleteState)
                }
            }
        }
    }

    fun updateChannelUri(channelId: Long, newUri: String) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    val updatedChannel = channel.copy(uri = newUri)
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, false, ChannelUpdatedType.Updated)
                }
            }
        }
    }

    fun messagesDeletedWithAutoDelete(channelId: Long, messageTIds: List<Long>) {
        val messageTIds = messageTIds.associateWith { true }
        cachedData.forEachKeyValue { _, value ->
            value[channelId]?.let { channel ->
                channel.lastMessage?.tid?.let {
                    if (messageTIds.containsKey(it)) {
                        val updatedChannel = channel.copy(lastMessage = null)
                        val diff = channel.diff(updatedChannel)
                        channelUpdatedFlow_.tryEmit(ChannelUpdateData(
                            channel = updatedChannel,
                            needSorting = true,
                            diff = diff,
                            eventType = ChannelUpdatedType.LastMessage)
                        )
                    }
                }
            }
        }
    }

    fun updatePinState(channelId: Long, pinnedAt: Long?) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    val updatedChannel = channel.copy(pinnedAt = pinnedAt)
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, true, ChannelUpdatedType.PinnedAt)
                }
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    val updatedChannel = channel.copy(newMessageCount = count.toLong(), unread = false)
                    val diff = channel.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, false, ChannelUpdatedType.UnreadCount)
                }
            }
        }
    }

    fun deleteChannel(vararg ids: Long) {
        synchronized(lock) {
            ids.forEach { id ->
                cachedData.forEach { (_, map) ->
                    map.remove(id)
                }
            }
            channelsDeletedFlow_.tryEmit(ids.toList())
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
            cachedData.forEachKeyValue { key, value ->
                if (key.isValidForConfig(newChannel)) {
                    value[newChannel.id] = newChannel
                }
            }
            // Adding pending channel id with real channel id for future getting real channel id by pending channel id
            fromPendingToRealChannelsData[pendingChannelId] = newChannel.id
            // Emitting to flow
            pendingChannelCreatedFlow_.tryEmit(Pair(pendingChannelId, newChannel))
        }
    }

    fun updateChannelDraftMessage(channelId: Long, draftMessage: DraftMessage?) {
        synchronized(lock) {
            cachedData.values.forEach { value ->
                value[channelId]?.let {
                    val updatedChannel = it.copy(draftMessage = draftMessage)
                    value[channelId] = updatedChannel
                    channelDraftMessageChangesFlow_.tryEmit(updatedChannel)
                }
            }
        }
    }

    fun updateChannelPeer(channelId: Long, user: SceytUser) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channelId]?.let { channel ->
                    var needToUpdate = false
                    val updatedChannel = channel.copy(
                        members = channel.members?.map { member ->
                            if (member.user.id == user.id) {
                                if (user.diff(member.user).hasDifference()) {
                                    needToUpdate = true
                                    member.copy(user = user.copy())
                                } else member
                            } else member
                        }
                    )
                    if (needToUpdate) {
                        val diff = channel.diff(updatedChannel)
                        channelUpdated(key, updatedChannel, diff, false, ChannelUpdatedType.Presence)
                    }
                }
            }
        }
    }

    fun removeChannelMessageReactions(channelId: Long, messageId: Long) {
        synchronized(lock) {
            cachedData.values.forEach { value ->
                value[channelId]?.let { channel ->
                    val filteredReactions = channel.newReactions?.filter { it.messageId != messageId }
                    val updatedChannel = channel.copy(newReactions = filteredReactions)
                    value[channelId] = updatedChannel
                }
            }
        }
    }

    fun channelLastReactionLoaded(channelId: Long) {
        synchronized(lock) {
            cachedData.values.forEach { value ->
                value[channelId]?.let { channel ->
                    channelReactionMsgLoadedFlow_.tryEmit(channel)
                }
            }
        }
    }

    fun onChannelMarkedAsReadOrUnread(channel: SceytChannel) {
        synchronized(lock) {
            cachedData.forEachKeyValue { key, value ->
                value[channel.id]?.let {
                    val updatedChannel = it.copy(
                        unread = channel.unread,
                        newMessageCount = channel.newMessageCount
                    )
                    val diff = it.diff(updatedChannel)
                    channelUpdated(key, updatedChannel, diff, false, ChannelUpdatedType.Updated)
                }
            }
        }
    }

    private fun channelUpdated(
            config: ChannelListConfig,
            channel: SceytChannel,
            diff: ChannelDiff,
            needSort: Boolean,
            type: ChannelUpdatedType,
    ) {
        getOrCreateMap(config)[channel.id] = channel
        channelUpdatedFlow_.tryEmit(ChannelUpdateData(channel, needSort, diff, type))
    }

    private fun channelAdded(channel: SceytChannel) {
        channelAddedFlow_.tryEmit(channel)
    }

    private fun putAndCheckHasDiff(config: ChannelListConfig, list: List<SceytChannel>): Boolean {
        var detectedDiff = false
        list.forEach {
            if (!detectedDiff) {
                val old = getOrCreateMap(config)[it.id]
                detectedDiff = old?.diff(it)?.hasDifference() ?: true
            }
            putToCache(config, it)
        }
        return detectedDiff
    }

    private fun putAndCheckHasDiff(config: ChannelListConfig, channel: SceytChannel): ChannelDiff {
        val map = getOrCreateMap(config)
        val old = map[channel.id]
        map[channel.id] = channel
        return old?.diff(channel) ?: ChannelDiff.DEFAULT
    }

    private fun putToCache(config: ChannelListConfig, vararg channel: SceytChannel) {
        channel.groupBy { it.pending }.forEach { group ->
            if (group.key)
                pendingChannelsData.putAll(group.value.associateBy { channel -> channel.id })
            else
                getOrCreateMap(config).putAll(group.value.associateBy { channel -> channel.id })
        }
    }

    private fun channelsByConfig(config: ChannelListConfig): List<SceytChannel> {
        return (cachedData[config]?.values ?: emptyList()).plus(
            pendingChannelsData.values.filter {
                config.isValidForConfig(it) && it.lastMessage != null
            }
        )
    }

    private fun getOrCreateMap(config: ChannelListConfig) = cachedData[config] ?: run {
        hashMapOf<Long, SceytChannel>().also {
            cachedData[config] = it
        }
    }

    private fun getChannelImpl(channelId: Long, config: ChannelListConfig? = null): SceytChannel? {
        return if (config != null)
            getOrCreateMap(config)[channelId] ?: pendingChannelsData[channelId]
        else {
            cachedData.forEach { (_, map) ->
                map[channelId]?.let { return it }
            }
            pendingChannelsData[channelId]
        }
    }

    private fun checkNeedSortByLastMessage(oldMsg: SceytMessage?, newMsg: SceytMessage?): Boolean {
        return oldMsg?.id != newMsg?.id || oldMsg?.createdAt != newMsg?.createdAt
    }
}