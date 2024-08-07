package com.sceyt.chatuikit.data.connectionobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.persistence.extensions.safeResume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

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

    suspend fun awaitToConnectSceytWithTimeout(timeout: Long): Boolean {
        if (isConnected)
            return true

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return suspendCancellableCoroutine { continuation ->
            onChangedConnectStatusFlow
                .onSubscription {
                    scope.launch {
                        delay(timeout)
                        continuation.safeResume(isConnected)
                        scope.cancel()
                    }
                }
                .onEach {
                    if (it.state == ConnectionState.Connected) {
                        continuation.safeResume(true)
                        scope.cancel()
                    }
                }.launchIn(scope)
        }
    }
}