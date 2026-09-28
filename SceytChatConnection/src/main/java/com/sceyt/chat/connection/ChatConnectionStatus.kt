package com.sceyt.chat.connection

import com.sceyt.chat.models.ConnectionState

data class ChatConnectionStatus(
    val userId: String? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isFetchingToken: Boolean = false,
    val error: Throwable? = null
)
