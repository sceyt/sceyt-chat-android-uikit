package com.sceyt.sceytchatuikit.data.connectionobserver

import com.sceyt.chat.models.Status
import com.sceyt.chat.models.Types

data class ConnectionStateData(
        val state: Types.ConnectState,
        val status: Status? = null
) {
    override fun equals(other: Any?): Boolean {
        return other is ConnectionStateData && state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}