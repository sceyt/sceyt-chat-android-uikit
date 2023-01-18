package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelsComparatorBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.*

class ChannelsCash {
    private var cashedData = hashMapOf<Long, SceytChannel>()
    private val lock = Any()

    companion object {
        private val channelUpdatedFlow_ = MutableSharedFlow<ChannelUpdateData>(
            extraBufferCapacity = 1,
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

        var currentChannelId: Long? = null
    }

    /** Added channels like upsert, and check is differences between channels*/
    fun addAll(list: List<SceytChannel>, checkDifference: Boolean): Boolean {
        synchronized(lock) {
            return if (checkDifference)
                putAndCheckHasDiff(list)
            else {
                cashedData.putAll(list.associateBy { it.id })
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
            cashedData.clear()
        }
    }

    fun getSorted(): List<SceytChannel> {
        synchronized(lock) {
            return cashedData.values.sortedWith(ChannelsComparatorBy()).map { it.clone() }
        }
    }

    fun get(channelId: Long): SceytChannel? {
        synchronized(lock) {
            return cashedData[channelId]?.clone()
        }
    }

    fun upsertChannel(vararg channels: SceytChannel) {
        synchronized(lock) {
            channels.forEach {
                if (cashedData[it.id] == null) {
                    cashedData[it.id] = it
                    channelAdded(it)
                } else {
                    val oldMsg = cashedData[it.id]?.lastMessage
                    if (putAndCheckHasDiff(it).hasDifference()) {
                        val needSort = checkNeedSortByLastMessage(oldMsg, it.lastMessage)
                        channelUpdated(it, needSort)
                    }
                }
            }
        }
    }

    fun updateLastMessage(channelId: Long, message: SceytMessage?) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                channel.lastMessage = message
                channelUpdated(channel, needSort)
            }
        }
    }

    fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                val needSort = checkNeedSortByLastMessage(channel.lastMessage, message)
                channel.lastMessage = message
                channel.lastReadMessageId = message.id
                channelUpdated(channel, needSort)
            }
        }
    }

    fun clearedHistory(channelId: Long) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                channel.lastMessage = null
                channel.unreadMessageCount = 0
                channelUpdated(channel, true)
            }
        }
    }

    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long = 0) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                if (muted) {
                    channel.muted = true
                    channel.muteExpireDate = Date(muteUntil)
                } else channel.muted = false

                channelUpdated(channel, false)
            }
        }
    }

    fun updateChannelSubjectAndAvatarUrl(channelId: Long, newSubject: String?, newUrl: String?) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                (channel as? SceytGroupChannel)?.let {
                    channel.subject = newSubject
                    channel.avatarUrl = newUrl

                    channelUpdated(channel, false)
                }
            }
        }
    }

    fun addedMembers(channelId: Long, sceytMember: SceytMember) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                (channel as? SceytGroupChannel)?.let {
                    it.members = it.members.toArrayList().apply {
                        add(sceytMember)
                    }
                    channelUpdated(channel, false)
                }
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                channel.unreadMessageCount = count.toLong()
                channel.markedUsUnread = false
                channelUpdated(channel, false)
            }
        }
    }

    fun deleteChannel(id: Long) {
        synchronized(lock) {
            cashedData.remove(id)
            channelDeletedFlow_.tryEmit(id)
        }
    }

    private fun channelUpdated(channel: SceytChannel, needSort: Boolean) {
        channelUpdatedFlow_.tryEmit(ChannelUpdateData(channel.clone(), needSort))
    }

    private fun channelAdded(channel: SceytChannel) {
        channelAddedFlow_.tryEmit(channel.clone())
    }

    fun updateMembersCount(channel: SceytGroupChannel) {
        cashedData[channel.id]?.let {
            (it as? SceytGroupChannel)?.memberCount = channel.memberCount
            channelUpdated(it, false)
        } ?: upsertChannel(channel)
    }

    private fun putAndCheckHasDiff(list: List<SceytChannel>): Boolean {
        var detectedDiff = false
        list.forEach {
            if (!detectedDiff) {
                val old = cashedData[it.id]
                detectedDiff = old?.diff(it)?.hasDifference() ?: true
            }
            cashedData[it.id] = it
        }
        return detectedDiff
    }

    private fun putAndCheckHasDiff(channel: SceytChannel): ChannelItemPayloadDiff {
        val old = cashedData[channel.id]
        cashedData[channel.id] = channel
        return old?.diff(channel) ?: ChannelItemPayloadDiff.DEFAULT
    }

    private fun checkNeedSortByLastMessage(oldMsg: SceytMessage?, newMsg: SceytMessage?): Boolean {
        return oldMsg?.id != newMsg?.id || oldMsg?.createdAt != newMsg?.createdAt
    }
}