package com.sceyt.sceytchatuikit

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
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
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCash
import com.sceyt.sceytchatuikit.pushes.SceytFirebaseMessagingDelegate
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.services.networkmonitor.ConnectionStateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

object SceytKitClient : SceytKoinComponent, CoroutineScope {
    private val application: Application by inject()
    private val preferences: SceytSharedPreference by inject()
    private val connectionStateService: ConnectionStateService by inject()
    private val database: SceytDatabase by inject()
    private val channelsCash: ChannelsCash by inject()
    private val persistenceChannelsMiddleWare by inject<PersistenceChanelMiddleWare>()
    private val persistenceMessagesMiddleWare by inject<PersistenceMessagesMiddleWare>()
    private val persistenceMembersMiddleWare by inject<PersistenceMembersMiddleWare>()
    private val persistenceUsersMiddleWare by inject<PersistenceUsersMiddleWare>()
    private val persistenceAttachmentsLogic by inject<PersistenceAttachmentLogic>()
    private val sceytSyncManager by inject<SceytSyncManager>()
    private val filesTransferService by inject<FileTransferService>()
    private val listenersMap = hashMapOf<String, (success: Boolean, errorMessage: String?) -> Unit>()

    val myId get() = preferences.getUserId()

    private val onTokenExpired_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenExpired = onTokenExpired_.asSharedFlow()

    private val onTokenWillExpire_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenWillExpire = onTokenWillExpire_.asSharedFlow()

    init {
        FirebaseApp.initializeApp(application)
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
        ConnectionEventsObserver.onChangedConnectStatusFlow.onEach {
            val connectStatus = it.state
            if (connectStatus == Types.ConnectState.StateConnected) {
                notifyState(true, null)

                val status = ClientWrapper.currentUser.presence.status
                ProcessLifecycleOwner.get().lifecycleScope.launchWhenResumed {
                    if (ConnectionEventsObserver.isConnected)
                        ClientWrapper.setPresence(PresenceState.Online, if (status.isNullOrBlank())
                            SceytKitConfig.presenceStatusText else status) {
                        }
                }
                SceytFirebaseMessagingDelegate.checkNeedRegisterForPushToken()
                launch {
                    persistenceMessagesMiddleWare.sendAllPendingMarkers()
                    persistenceMessagesMiddleWare.sendAllPendingMessages()
                }
                sceytSyncManager.startSync()
            } else if (connectStatus == Types.ConnectState.StateFailed) {
                notifyState(false, it.status?.error?.message)
            } else if (connectStatus == Types.ConnectState.StateDisconnect) {
                if (it.status?.error?.code == 40102)
                    onTokenExpired_.tryEmit(Unit)
                else notifyState(false, it.status?.error?.message)
            }
        }.launchIn(this)

        ConnectionEventsObserver.onTokenExpired.onEach {
            onTokenExpired_.tryEmit(Unit)
        }.launchIn(this)

        ConnectionEventsObserver.onTokenWillExpire.onEach {
            onTokenWillExpire_.tryEmit(Unit)
        }.launchIn(this)

        (this as CoroutineScope).coroutineContext

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

    fun getAttachmentsLogic() = persistenceAttachmentsLogic

    fun getMembersMiddleWare() = persistenceMembersMiddleWare

    fun getUserMiddleWare() = persistenceUsersMiddleWare

    fun getSyncManager() = sceytSyncManager

    fun getFileTransferService() = filesTransferService

    fun addListener(key: String, listener: (success: Boolean, errorMessage: String?) -> Unit) {
        listenersMap[key] = listener
    }

    fun clearData() {
        database.clearAllTables()
        preferences.clear()
        channelsCash.clear()
    }

    fun logOut(listener: ((success: Boolean, errorMessage: String?) -> Unit)? = null) {
        ChatClient.getClient().unregisterPushToken(object : ActionCallback {
            override fun onSuccess() {
                clearData()
                ChatClient.getClient().disconnect()
                listener?.invoke(true, null)
            }

            override fun onError(exception: SceytException?) {
                listener?.invoke(false, exception?.message)
            }
        })
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}