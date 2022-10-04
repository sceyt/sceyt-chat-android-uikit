package com.sceyt.sceytchatuikit

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionObserver
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.services.networkmonitor.ConnectionStateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.inject

object SceytKitClient : SceytKoinComponent {
    private val preferences: SceytSharedPreference by inject()
    private val connectionStateService: ConnectionStateService by inject()
    private val database: SceytDatabase by inject()
    private val scope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

    private val onTokenExpired_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenExpired = onTokenExpired_.asSharedFlow()

    fun connect(token: String, userName: String,
                successListener: ((success: Boolean, errorMessage: String?) -> Unit)? = null) {

        preferences.setUserId(userName)
        getChatClient()?.connect(token)
        addListener(successListener)
    }

    fun reconnect() {
        getChatClient()?.reconnect()
    }

    fun disconnect() {
        getChatClient()?.disconnect()
    }

    private fun addListener(listener: ((success: Boolean, errorMessage: String?) -> Unit)?) {
        scope.launch {
            ConnectionObserver.onChangedConnectStatusFlow.collect {
                val connectStatus = it.first
                if (connectStatus == Types.ConnectState.StateConnected) {
                    listener?.invoke(true, null)
                    ClientWrapper.setPresence(PresenceState.Online, "") {

                    }
                } else if (connectStatus == Types.ConnectState.StateFailed) {
                    listener?.invoke(false, it.second?.error?.message)
                } else if (connectStatus == Types.ConnectState.StateDisconnect) {
                    if (it.second?.error?.code == 40102)
                        onTokenExpired_.tryEmit(Unit)
                    else listener?.invoke(false, it.second?.error?.message)
                }
            }
        }

        scope.launch {
            ConnectionObserver.onTokenExpired.collect {
                onTokenExpired_.tryEmit(Unit)
            }
        }

        scope.launch {
            ConnectionObserver.onTokenWillExpire.collect {
                onTokenExpired_.tryEmit(Unit)
            }
        }
    }

    private fun getChatClient(): ChatClient? = ChatClient.getClient()

    fun getConnectionService() = connectionStateService

    fun clearData() {
        database.clearAllTables()
    }
}