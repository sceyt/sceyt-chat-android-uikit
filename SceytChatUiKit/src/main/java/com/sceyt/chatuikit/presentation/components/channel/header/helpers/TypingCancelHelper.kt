package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.presentation.common.DebounceHelper

class TypingCancelHelper {

    private val debounceHelpers: HashMap<Long, DebounceHelper> by lazy { hashMapOf() }

    fun await(data: ChannelMemberActivityEvent, callBack: (ChannelMemberActivityEvent) -> Unit) {
        if (data.active.not()) {
            debounceHelpers[data.channelId]?.cancelLastDebounce()
            return
        }
        debounceHelpers[data.channelId]?.submit {
            callBack.invoke(data.inverse())
        } ?: run {
            val debounceHelper = DebounceHelper(5000)
            debounceHelpers[data.channelId] = debounceHelper
            debounceHelper.submit {
                callBack.invoke(data.inverse())
            }
        }
    }
}