package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelsComparatorBy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.*

class ChannelsCash {
    private var cashedData = hashMapOf<Long, SceytChannel>()
    private val lock = Any()

    companion object {
        private val channelUpdatedFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelUpdatedFlow: SharedFlow<SceytChannel> = channelUpdatedFlow_
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
            if (putAndCheckHasDiff(arrayListOf(channel))) {
                channelUpdated(channel)
            }
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

    fun updateChannel(vararg channels: SceytChannel) {
        synchronized(lock) {
            channels.forEach {
                if (putAndCheckHasDiff(arrayListOf(it))) {
                    channelUpdated(it)
                }
            }
        }
    }

    fun updateLastMessage(channelId: Long, lastMessage: SceytMessage) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                channel.lastMessage = lastMessage
                channelUpdated(channel.clone())
            }
        }
    }

    fun clearedHistory(channelId: Long) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                channel.lastMessage = null
                channel.unreadMessageCount = 0
                channelUpdated(channel.clone())
            }
        }
    }

    fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                channel.lastMessage = message
                channel.lastReadMessageId = message.id
                channelUpdated(channel.clone())
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

                channelUpdated(channel)
            }
        }
    }

    fun updateChannelSubjectAndAvatarUrl(channelId: Long, newSubject: String?, newUrl: String?) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                (channel as? SceytGroupChannel)?.let {
                    channel.subject = newSubject
                    channel.avatarUrl = newUrl

                    channelUpdated(channel)
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

                    channelUpdated(channel)
                }
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(lock) {
            cashedData[channelId]?.let { channel ->
                channel.unreadMessageCount = count.toLong()
                channel.markedUsUnread = false
                channelUpdated(channel)
            }
        }
    }

    fun deleteChannel(id: Long) {
        synchronized(lock) {
            cashedData.remove(id)
        }
    }

    private fun channelUpdated(channel: SceytChannel) {
        channelUpdatedFlow_.tryEmit(channel.clone())
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
}