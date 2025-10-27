package com.sceyt.chatuikit.presentation.components.channel.messages.events

import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage

sealed class PollEvent {

    data class ToggleVote(
        val message: SceytMessage,
        val option: PollOption
    ) : PollEvent()
}

