package com.sceyt.sceytchatuikit.data.connectionobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.extensions.TAG
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull

object ConnectionEventsObserver {
    val connectionState get() = ClientWrapper.getConnectionState() ?: ConnectionState.Disconnected
    val isConnected get() = connectionState == ConnectionState.Connected

    private val onChangedConnectStatusFlow_: MutableSharedFlow<ConnectionStateData> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onChangedConnectStatusFlow = onChangedConnectStatusFlow_.asSharedFlow()

    private val onTokenWillExpire_: MutableSharedFlow<Long> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenWillExpire = onTokenWillExpire_.asSharedFlow()

    private val onTokenExpired_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenExpired = onTokenExpired_.asSharedFlow()

    init {
        ChatClient.getClient().addClientListener(TAG, object : ClientListener {
            override fun onConnectionStateChanged(state: ConnectionState?, exception: SceytException?) {
                onChangedConnectStatusFlow_.tryEmit(ConnectionStateData(state, exception))
            }

            override fun onTokenWillExpire(expireTime: Long) {
                onTokenWillExpire_.tryEmit(expireTime)
            }

            override fun onTokenExpired() {
                onTokenExpired_.tryEmit(Unit)
            }
        })
    }

    suspend fun awaitToConnectSceyt(): Boolean {
        if (isConnected)
            return true

        onChangedConnectStatusFlow.firstOrNull { it.state == ConnectionState.Connected }
        return connectionState == ConnectionState.Connected
    }
}