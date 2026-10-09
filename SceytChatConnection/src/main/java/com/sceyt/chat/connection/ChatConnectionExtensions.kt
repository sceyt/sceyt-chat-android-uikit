package com.sceyt.chat.connection

import com.sceyt.chat.models.ConnectionState

internal val ConnectionState.isDisconnected: Boolean
    get() = this == ConnectionState.Disconnected || this == ConnectionState.Failed

internal val ConnectionState.isActive: Boolean
    get() = when (this) {
        ConnectionState.Connecting,
        ConnectionState.Reconnecting,
        ConnectionState.Connected -> true

        ConnectionState.Disconnected,
        ConnectionState.Failed -> false
    }

internal fun ChatConnectionClient.isConnectedAs(userId: String?): Boolean =
    userId != null &&
        connectionState == ConnectionState.Connected &&
        connectedUserId == userId
