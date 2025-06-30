package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.input.data.UserActivity
import java.util.concurrent.ConcurrentHashMap

class UserActivityCancelHelper {

    private val debounceHelpers: ConcurrentHashMap<Long, DebounceHelper> by lazy { ConcurrentHashMap() }

    fun await(event: ChannelMemberActivityEvent, callback: (ChannelMemberActivityEvent) -> Unit) {
        if (!event.active) {
            debounceHelpers[getKey(event)]?.cancelLastDebounce()
            return
        }

        debounceHelpers.compute(getKey(event)) { _, existingHelper ->
            val helper = existingHelper ?: DebounceHelper(5000)
            helper.submit {
                callback.invoke(event.inverse())
            }
            helper
        }
    }

    private fun getKey(event: ChannelMemberActivityEvent): Long {
        return when (event.activity) {
            UserActivity.Typing -> event.channelId + event.userId.hashCode() + 1L
            UserActivity.Recording -> event.channelId + event.userId.hashCode() + 2L
        }
    }
}