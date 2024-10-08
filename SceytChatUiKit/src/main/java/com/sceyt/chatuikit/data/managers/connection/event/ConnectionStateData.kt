package com.sceyt.chatuikit.data.managers.connection.event

import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException

data class ConnectionStateData(
        val state: ConnectionState?,
        val exception: SceytException? = null
) {
    override fun equals(other: Any?): Boolean {
        return other is ConnectionStateData && state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}