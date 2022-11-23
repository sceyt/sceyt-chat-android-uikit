package com.sceyt.sceytchatuikit

import com.sceyt.chat.ChatClient
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.Types
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.*
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
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
    private val channelsCash: ChannelsCash by inject()
    private val scope by lazy { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    private val persistenceChannelsMiddleWare by inject<PersistenceChanelMiddleWare>()
    private val persistenceMessagesMiddleWare by inject<PersistenceMessagesMiddleWare>()
    private val persistenceMembersMiddleWare by inject<PersistenceMembersMiddleWare>()
    private val persistenceUsersMiddleWare by inject<PersistenceUsersMiddleWare>()
    private val syncManager by inject<SceytSyncManager>()
    private val listenersMap = hashMapOf<String, (success: Boolean, errorMessage: String?) -> Unit>()

    private val onTokenExpired_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenExpired = onTokenExpired_.asSharedFlow()

    private val onTokenWillExpire_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenWillExpire = onTokenWillExpire_.asSharedFlow()

    init {
        setListener()
    }

    fun connect(token: String, userName: String) {
        preferences.setUserId(userName)
        getChatClient()?.connect(token)
    }

    fun updateToken(token: String, listener: ((success: Boolean, errorMessage: String?) -> Unit)? = null) {
        ChatClient.updateToken(token, object : ActionCallback {
            override fun onSuccess() {
                listener?.invoke(true, null)
            }

            override fun onError(error: SceytException?) {
                listener?.invoke(false, error?.message)
            }
        })
    }

    fun reconnect() {
        getChatClient()?.reconnect()
    }

    fun disconnect() {
        getChatClient()?.disconnect()
    }

    private fun setListener() {
        scope.launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.collect {
                val connectStatus = it.state
                if (connectStatus == Types.ConnectState.StateConnected) {
                    notifyState(true, null)

                    val status = ClientWrapper.currentUser.presence.status
                    ClientWrapper.setPresence(PresenceState.Online, if (status.isNullOrBlank())
                        SceytKitConfig.presenceStatusText else status) {
                    }
                    persistenceMessagesMiddleWare.sendAllPendingMarkers()
                    persistenceMessagesMiddleWare.sendAllPendingMessages()
                    syncManager.startSync()
                } else if (connectStatus == Types.ConnectState.StateFailed) {
                    notifyState(false, it.status?.error?.message)
                } else if (connectStatus == Types.ConnectState.StateDisconnect) {
                    if (it.status?.error?.code == 40102)
                        onTokenExpired_.tryEmit(Unit)
                    else notifyState(false, it.status?.error?.message)
                }
            }
        }

        scope.launch {
            ConnectionEventsObserver.onTokenExpired.collect {
                onTokenExpired_.tryEmit(Unit)
            }
        }

        scope.launch {
            ConnectionEventsObserver.onTokenWillExpire.collect {
                onTokenWillExpire_.tryEmit(Unit)
            }
        }
    }

    private fun notifyState(success: Boolean, errorMessage: String?) {
        listenersMap.values.forEach { listener ->
            listener.invoke(success, errorMessage)
        }
    }

    private fun getChatClient(): ChatClient? = ChatClient.getClient()

    fun getConnectionService() = connectionStateService

    fun getChannelsMiddleWare() = persistenceChannelsMiddleWare

    fun getMessagesMiddleWare() = persistenceMessagesMiddleWare

    fun getMembersMiddleWare() = persistenceMembersMiddleWare

    fun getUserMiddleWare() = persistenceUsersMiddleWare

    fun addListener(key: String, listener: (success: Boolean, errorMessage: String?) -> Unit) {
        listenersMap[key] = listener
    }

    fun clearData() {
        database.clearAllTables()
        preferences.clear()
        channelsCash.clear()
    }
}