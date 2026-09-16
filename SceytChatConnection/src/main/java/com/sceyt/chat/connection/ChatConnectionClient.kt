package com.sceyt.chat.connection

import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException

internal interface ChatConnectionClient {
    val isReadyForConnection: Boolean

    val connectionState: ConnectionState

    val connectedUserId: String?

    fun setListener(listener: Listener)

    fun connect(token: String)

    fun disconnect()

    suspend fun updateToken(token: String): Result<Unit>

    interface Listener {
        fun onConnectionStateChanged(state: ConnectionState, error: SceytException?)

        fun onTokenWillExpire()

        fun onTokenExpired()
    }
}
