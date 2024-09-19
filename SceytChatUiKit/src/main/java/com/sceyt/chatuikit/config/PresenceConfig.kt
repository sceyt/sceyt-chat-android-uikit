package com.sceyt.chatuikit.config

import com.sceyt.chat.models.user.PresenceState

data class PresenceConfig(
        val defaultPresenceState: PresenceState = PresenceState.Online,
        val defaultPresenceStatus: String = ""
)