package com.sceyt.chatuikit.data.managers.connection

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_listeners.ClientListener
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

object ConnectionEventManager {
    private const val TAG = "ConnectionEventManager"

    val connectionState: ConnectionState
        get() = getConnectionStateIfInitialized()

    val isConnected get() = connectionState == ConnectionState.Connected
    val isConnecting get() = connectionState == ConnectionState.Connecting

    private val _onChangedConnectStatusFlow: MutableSharedFlow<ConnectionStateData> =
        MutableSharedFlow(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            replay = 1
        )
    val onChangedConnectStatusFlow = _onChangedConnectStatusFlow.asSharedFlow()

    private val _onTokenWillExpire: MutableSharedFlow<Long> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onTokenWillExpire = _onTokenWillExpire.asSharedFlow()

    private val _onTokenExpired: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onTokenExpired = _onTokenExpired.asSharedFlow()

    init {
        ChatClient.getClient().addClientListener(TAG, object : ClientListener {
            override fun onConnectionStateChanged(
                state: ConnectionState?,
                exception: SceytException?
            ) {
                SceytLog.i(TAG, "onConnectionStateChanged, state: $state, exception: $exception")
                _onChangedConnectStatusFlow.tryEmit(ConnectionStateData(state, exception))
            }

            override fun onTokenWillExpire(expireTime: Long) {
                _onTokenWillExpire.tryEmit(expireTime)
            }

            override fun onTokenExpired() {
                _onTokenExpired.tryEmit(Unit)
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

        return withTimeoutOrNull(timeout) {
            onChangedConnectStatusFlow
                .first { it.state == ConnectionState.Connected }
            true
        } ?: isConnected
    }

    suspend fun awaitToConnectSceytWithResult(timeout: Long): Result<Boolean> {
        if (isConnected)
            return Result.success(true)

        val state = withTimeoutOrNull(timeout) {
            onChangedConnectStatusFlow.first { data ->
                data.state == ConnectionState.Connected || data.state == ConnectionState.Failed
            }
        }

        return when (state?.state) {
            ConnectionState.Connected -> Result.success(true)
            ConnectionState.Failed -> Result.failure(
                state.exception ?: Exception("Connection failed")
            )

            else -> Result.failure(Exception("Connection timeout"))
        }
    }

    private fun getConnectionStateIfInitialized(): ConnectionState {
        return if (ChatClient.isInitialized()) {
            ChatClient.getClient().connectionState()
        } else ConnectionState.Disconnected
    }

    internal fun onDisconnected() {
        _onChangedConnectStatusFlow.tryEmit(ConnectionStateData(ConnectionState.Disconnected))
    }
}