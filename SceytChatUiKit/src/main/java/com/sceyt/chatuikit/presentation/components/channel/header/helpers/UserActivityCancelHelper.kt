package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import java.util.concurrent.ConcurrentHashMap

class UserActivityCancelHelper {

    private val debounceHelpers: ConcurrentHashMap<Long, DebounceHelper> by lazy { ConcurrentHashMap() }

    fun await(event: ChannelMemberActivityEvent, callBack: (ChannelMemberActivityEvent) -> Unit) {
        if (!event.active) {
            debounceHelpers[getKey(event)]?.cancelLastDebounce()
            return
        }

        debounceHelpers.compute(getKey(event)) { _, existingHelper ->
            val helper = existingHelper ?: DebounceHelper(15000)
            helper.submit {
                callBack.invoke(event.inverse())
            }
            helper
        }
    }

    private fun getKey(event: ChannelMemberActivityEvent): Long {
        return when (event) {
            is ChannelMemberActivityEvent.Typing -> event.channelId.plus(1)
            is ChannelMemberActivityEvent.Recording -> event.channelId.plus(2)
        }
    }
}