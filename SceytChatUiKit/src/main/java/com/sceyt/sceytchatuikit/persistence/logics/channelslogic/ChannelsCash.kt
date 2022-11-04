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
    private val syncOb = Any()

    companion object {
        private val channelUpdatedFlow_ = MutableSharedFlow<SceytChannel>(
            extraBufferCapacity = 5,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val channelUpdatedFlow: SharedFlow<SceytChannel> = channelUpdatedFlow_
    }

    /** Added channels like upsert, and check is differences between channels*/
    fun addAll(list: List<SceytChannel>, checkDifference: Boolean): Boolean {
        synchronized(syncOb) {
            return if (checkDifference)
                putAndCheckHasDiff(list)
            else {
                cashedData.putAll(list.associateBy { it.id })
                false
            }
        }
    }

    fun add(channel: SceytChannel) {
        synchronized(syncOb) {
            if (putAndCheckHasDiff(arrayListOf(channel))) {
                channelUpdatedFlow_.tryEmit(channel)
            }
        }
    }

    fun clear() {
        synchronized(syncOb) {
            cashedData.clear()
        }
    }

    fun getSorted(): List<SceytChannel> {
        synchronized(syncOb) {
            return cashedData.values.sortedWith(ChannelsComparatorBy()).map { it.clone() }
        }
    }

    fun updateChannel(vararg channels: SceytChannel) {
        synchronized(syncOb) {
            channels.forEach {
                if (putAndCheckHasDiff(arrayListOf(it))) {
                    channelUpdatedFlow_.tryEmit(it)
                }
            }
        }
    }

    fun updateLastMessage(channelId: Long, lastMessage: SceytMessage?) {
        synchronized(syncOb) {
            cashedData[channelId]?.let { channel ->
                channel.lastMessage = lastMessage
                channelUpdatedFlow_.tryEmit(channel)
            }
        }
    }

    fun updateMuteState(channelId: Long, muted: Boolean, muteUntil: Long = 0) {
        synchronized(syncOb) {
            cashedData[channelId]?.let { channel ->
                if (muted) {
                    channel.muted = true
                    channel.muteExpireDate = Date(muteUntil)
                } else channel.muted = false

                channelUpdatedFlow_.tryEmit(channel)
            }
        }
    }

    fun updateChannelSubjectAndAvatarUrl(channelId: Long, newSubject: String?, newUrl: String?) {
        synchronized(syncOb) {
            cashedData[channelId]?.let { channel ->
                (channel as? SceytGroupChannel)?.let {
                    channel.subject = newSubject
                    channel.avatarUrl = newUrl

                    channelUpdatedFlow_.tryEmit(channel)
                }
            }
        }
    }

    fun addedMembers(channelId: Long, sceytMember: SceytMember) {
        synchronized(syncOb) {
            cashedData[channelId]?.let { channel ->
                (channel as? SceytGroupChannel)?.let {
                    it.members = it.members.toArrayList().apply {
                        add(sceytMember)
                    }

                    channelUpdatedFlow_.tryEmit(channel)
                }
            }
        }
    }

    fun updateUnreadCount(channelId: Long, count: Int) {
        synchronized(syncOb) {
            cashedData[channelId]?.let { channel ->
                channel.unreadMessageCount = count.toLong()
                channel.markedUsUnread = false
                channelUpdatedFlow_.tryEmit(channel)
            }
        }
    }

    fun deleteChannel(id: Long) {
        synchronized(syncOb) {
            cashedData.remove(id)
        }
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