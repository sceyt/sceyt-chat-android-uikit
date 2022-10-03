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
import kotlinx.coroutines.launch
import org.koin.core.component.inject

object SceytKitClient : SceytKoinComponent {
    private val preferences: SceytSharedPreference by inject()
    private val connectionStateService: ConnectionStateService by inject()
    private val database: SceytDatabase by inject()
    private val scope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob()) }

    fun connect(token: String, userName: String,
                successListener: ((success: Boolean, errorMessage: String?) -> Unit)? = null) {

        preferences.setUserId(userName)
        getChatClient()?.connect(token)
        addListener(successListener, token, userName)
    }

    fun reconnect() {
        getChatClient()?.reconnect()
    }

    fun disconnect() {
        getChatClient()?.disconnect()
    }

    private fun addListener(success: ((success: Boolean, errorMessage: String?) -> Unit)?, token: String, username: String) {
        scope.launch {
            ConnectionObserver.onChangedConnectStatusFlow.collect {
                val connectStatus = it.first
                if (connectStatus == Types.ConnectState.StateConnected) {
                    success?.invoke(true, null)
                    ClientWrapper.setPresence(PresenceState.Online, "") {

                    }
                } else if (connectStatus == Types.ConnectState.StateFailed)
                    success?.invoke(false, it.second?.error?.message)
                else if (connectStatus == Types.ConnectState.StateDisconnect) {
                    if (it.second?.error?.code == 40102)
                        connect(token, username)
                    else success?.invoke(false, it.second?.error?.message)
                }
            }
        }

        scope.launch {
            ConnectionObserver.onTokenExpired.collect {
                connect(token, username)
            }
        }

        scope.launch {
            ConnectionObserver.onTokenWillExpire.collect {
                connect(token, username)
            }
        }
    }

    private fun getChatClient(): ChatClient? = ChatClient.getClient()

    fun getConnectionService() = connectionStateService

    fun clearData() {
        database.clearAllTables()
    }
}