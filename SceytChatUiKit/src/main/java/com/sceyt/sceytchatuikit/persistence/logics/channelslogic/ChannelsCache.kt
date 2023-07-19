package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import androidx.lifecycle.MutableLiveData
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.hasDiff
import com.sceyt.sceytchatuikit.data.models.channels.DraftMessage
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.extensions.asLiveData
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelsComparatorBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.Date

class ChannelsCache {
    private var cachedData = hashMapOf<Long, SceytChannel>()
    private val lock = Any()

    companion object {
        private val channelUpdatedFlow_ = MutableSharedFlow<ChannelUpdateData>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelUpdatedFlow: SharedFlow<ChannelUpdateData> = channelUpdatedFlow_

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

        private val channelDraftMessageChangesLiveData_ = MutableLiveData<Pair<Long, DraftMessage?>>()
        val channelDraftMessageChangesLiveData = channelDraftMessageChangesLiveData_.asLiveData()

        var currentChannelId: Long? = null
    }

    /** Added channels like upsert, and check is differences between channels*/
    fun addAll(list: List<SceytChannel>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(list)
            else {
                cachedData.putAll(list.map { it.clone() }.associateBy { it.id })
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

    fun clear() {
        synchronized(lock) {
            cachedData.clear()
        }
    }

    fun getSorted(): List<SceytChannel> {
        synchronized(lock) {
            return cachedData.values.sortedWith(ChannelsComparatorBy()).map { it.clone() }
        }
    }

    fun getData(): List<SceytChannel> {
        synchronized(lock) {
            return cachedData.values.map { it.clone() }
        }
    }

    fun get(channelId: Long): SceytChannel? {
        synchronized(lock) {
            return cachedData[channelId]?.clone()
        }
    }

    fun upsertChannel(vararg channels: SceytChannel) {
        synchronized(lock) {
            channels.forEach {
                if (cachedData[it.id] == null) {
                    cachedData[it.id] = it.clone()
                    channelAdded(it)
                } else {
                    val oldMsg = cachedData[it.id]?.lastMessage
                    if (putAndCheckHasDiff(it).hasDifference()) {
                        val needSort = checkNeedSortByLastMessage(oldMsg, it.lastMessage)
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
                channel.lastReadMessageId = message.id
                channelUpdated(channel, needSort, ChannelUpdatedType.LastMessage)
            }
        }
    }

    fun clearedHistory(channelId: Long) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channel.lastMessage = null
                channel.unreadMessageCount = 0
                channel.unreadMentionCount = 0
                channel.unreadReactionCount = 0
                channel.userMessageReactions = null
                channelUpdated(channel, true, ChannelUpdatedType.ClearedHistory)
            }
        }
    }

    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long = 0) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                if (muted) {
                    channel.muted = true
                    channel.muteExpireDate = Date(muteUntil)
                } else channel.muted = false

                channelUpdated(channel, false, ChannelUpdatedType.MuteState)
            }
        }
    }

    fun addedMembers(channelId: Long, sceytMember: SceytMember) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                (channel as? SceytGroupChannel)?.let {
                    it.members = it.members.toArrayList().apply {
                        add(sceytMember.copy())
                    }
                    channelUpdated(channel, false, ChannelUpdatedType.Members)
                }
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(lock) {
            cachedData[channelId]?.let { channel ->
                channel.unreadMessageCount = count.toLong()
                channel.markedUsUnread = false
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

    private fun channelUpdated(channel: SceytChannel, needSort: Boolean, type: ChannelUpdatedType) {
        channelUpdatedFlow_.tryEmit(ChannelUpdateData(channel.clone(), needSort, type))
    }

    private fun channelAdded(channel: SceytChannel) {
        channelAddedFlow_.tryEmit(channel.clone())
    }

    fun updateMembersCount(channel: SceytGroupChannel) {
        cachedData[channel.id]?.let {
            (it as? SceytGroupChannel)?.memberCount = channel.memberCount
            channelUpdated(it, false, ChannelUpdatedType.Members)
        } ?: upsertChannel(channel)
    }

    fun updateChannelDraftMessage(channelId: Long, draftMessage: DraftMessage?) {
        cachedData[channelId]?.let {
            it.draftMessage = draftMessage?.copy()
            channelDraftMessageChangesLiveData_.postValue(Pair(channelId, draftMessage))
        }
    }

    fun updateChannelPeer(id: Long, user: User) {
        synchronized(lock) {
            cachedData[id]?.let {
                (it as? SceytDirectChannel)?.let { channel ->
                    val oldUser = channel.peer?.user
                    if (oldUser?.presence?.hasDiff(user.presence) == true) {
                        channel.peer?.user = user
                        channelUpdated(channel, false, ChannelUpdatedType.Presence)
                    }
                }
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
            cachedData[it.id] = it.clone()
        }
        return detectedDiff
    }

    private fun putAndCheckHasDiff(channel: SceytChannel): ChannelItemPayloadDiff {
        val old = cachedData[channel.id]
        cachedData[channel.id] = channel.clone()
        return old?.diff(channel) ?: ChannelItemPayloadDiff.DEFAULT
    }

    private fun checkNeedSortByLastMessage(oldMsg: SceytMessage?, newMsg: SceytMessage?): Boolean {
        return oldMsg?.id != newMsg?.id || oldMsg?.createdAt != newMsg?.createdAt
    }
}