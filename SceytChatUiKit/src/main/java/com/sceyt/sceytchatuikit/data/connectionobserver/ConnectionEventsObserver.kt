package com.sceyt.sceytchatuikit.data.connectionobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.models.Status
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.extensions.safeResume
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ConnectionEventsObserver {
    val connectionState get() = ClientWrapper.connectState ?: Types.ConnectState.StateDisconnect
    val isConnected get() = connectionState == Types.ConnectState.StateConnected

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
            override fun onChangedConnectStatus(connectStatus: Types.ConnectState, status: Status?) {
                onChangedConnectStatusFlow_.tryEmit(ConnectionStateData(connectStatus, status))
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

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return suspendCancellableCoroutine { continuation ->
            scope.launch {
                onChangedConnectStatusFlow.collect {
                    if (it.state == Types.ConnectState.StateConnected) {
                        continuation.safeResume(true)
                        scope.cancel()
                    }
                }
            }
        }
    }
}