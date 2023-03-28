package com.sceyt.sceytchatuikit

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.pushes.SceytFirebaseMessagingDelegate
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
    private val channelsCache: ChannelsCache by inject()
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
            when (it.state) {
                ConnectionState.Connected -> {
                    notifyState(true, null)
                    launch {
                        ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            if (ConnectionEventsObserver.isConnected)
                                persistenceUsersMiddleWare.setPresenceState(PresenceState.Online)
                        }
                    }
                    SceytFirebaseMessagingDelegate.checkNeedRegisterForPushToken()
                    launch(Dispatchers.IO) {
                        persistenceMessagesMiddleWare.sendAllPendingMarkers()
                        persistenceMessagesMiddleWare.sendAllPendingMessages()
                    }
                    sceytSyncManager.startSync()
                }
                ConnectionState.Failed -> {
                    notifyState(false, it.exception?.message)
                }
                ConnectionState.Disconnected -> {
                    if (it.exception?.code == 40102)
                        onTokenExpired_.tryEmit(Unit)
                    else notifyState(false, it.exception?.message)
                }
                else -> {}
            }
        }.launchIn(this)

        ConnectionEventsObserver.onTokenExpired.onEach {
            onTokenExpired_.tryEmit(Unit)
        }.launchIn(this)

        ConnectionEventsObserver.onTokenWillExpire.onEach {
            onTokenWillExpire_.tryEmit(Unit)
        }.launchIn(this)
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
        channelsCache.clear()
    }

    fun logOut(unregisterPushCallback: ((success: Boolean, errorMessage: String?) -> Unit)? = null) {
        clearData()
        WorkManager.getInstance(application).cancelAllWork()
        ClientWrapper.currentUser = null
        ChatClient.getClient().unregisterPushToken(object : ActionCallback {
            override fun onSuccess() {
                ChatClient.getClient().disconnect()
                unregisterPushCallback?.invoke(true, null)
            }

            override fun onError(exception: SceytException?) {
                unregisterPushCallback?.invoke(false, exception?.message)
            }
        })
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}