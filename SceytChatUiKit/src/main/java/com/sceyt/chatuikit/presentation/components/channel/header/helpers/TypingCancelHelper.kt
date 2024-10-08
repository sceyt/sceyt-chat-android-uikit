package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.managers.channel.event.ChannelTypingEventData
import com.sceyt.chatuikit.presentation.common.DebounceHelper

class TypingCancelHelper {

    private val debounceHelpers: HashMap<Long, DebounceHelper> by lazy { hashMapOf() }

    fun await(data: ChannelTypingEventData, callBack: (ChannelTypingEventData) -> Unit) {
        if (data.typing.not()) {
            debounceHelpers[data.channel.id]?.cancelLastDebounce()
            return
        }
        debounceHelpers[data.channel.id]?.submit {
            callBack.invoke(data.copy(typing = false))
        } ?: run {
            val debounceHelper = DebounceHelper(5000)
            debounceHelpers[data.channel.id] = debounceHelper
            debounceHelper.submit {
                callBack.invoke(data.copy(typing = false))
            }
        }
    }
}