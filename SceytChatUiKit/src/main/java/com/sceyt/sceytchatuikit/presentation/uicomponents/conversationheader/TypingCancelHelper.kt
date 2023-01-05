package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader

import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.presentation.uicomponents.searchinput.DebounceHelper

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