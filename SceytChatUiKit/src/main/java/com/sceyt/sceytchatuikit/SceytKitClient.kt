package com.sceyt.sceytchatuikit

import android.content.Context
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
import com.sceyt.sceytchatuikit.persistence.PersistenceAttachmentsMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceUsersMiddleWare
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.pushes.SceytFirebaseMessagingDelegate
import com.sceyt.sceytchatuikit.services.networkmonitor.ConnectionStateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.inject
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

object SceytKitClient : SceytKoinComponent, CoroutineScope {
    private val context: Context by inject()
    private val preferences: SceytSharedPreference by inject()
    private val connectionStateService: ConnectionStateService by inject()
    private val database: SceytDatabase by inject()
    private val channelsCache: ChannelsCache by inject()
    private val persistenceChannelsMiddleWare by inject<PersistenceChanelMiddleWare>()
    private val persistenceMessagesMiddleWare by inject<PersistenceMessagesMiddleWare>()
    private val persistenceMembersMiddleWare by inject<PersistenceMembersMiddleWare>()
    private val persistenceUsersMiddleWare by inject<PersistenceUsersMiddleWare>()
    private val persistenceAttachmentsMiddleWare by inject<PersistenceAttachmentsMiddleWare>()
    private val sceytSyncManager by inject<SceytSyncManager>()
    private val filesTransferService by inject<FileTransferService>()
    private val globalScope by inject<CoroutineScope>()
    private val listenersMap = hashMapOf<String, (success: Boolean, errorMessage: String?) -> Unit>()
    private var clientUserId: String? = null

    val myId
        get() = clientUserId ?: preferences.getUserId().also {
            clientUserId = it
        }

    private val onTokenExpired_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenExpired = onTokenExpired_.asSharedFlow()

    private val onTokenWillExpire_: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenWillExpire = onTokenWillExpire_.asSharedFlow()

    init {
        FirebaseApp.initializeApp(context)
        setListener()
    }

    @JvmStatic
    fun connect(token: String) {
        getChatClient()?.connect(token)
    }

    @JvmStatic
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

    @JvmStatic
    fun reconnect() {
        getChatClient()?.reconnect()
    }

    @JvmStatic
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
                        persistenceMessagesMiddleWare.sendAllPendingMessageStateUpdates()
                        persistenceMessagesMiddleWare.sendAllPendingReactions()
                        if (!channelsCache.initialized)
                            delay(1000) // Await 1 second maybe channel cache will be initialized
                        sceytSyncManager.startSync(false)
                    }
                }

                ConnectionState.Failed -> {
                    notifyState(false, it.exception?.message)
                }

                ConnectionState.Disconnected -> {
                    notifyState(false, it.exception?.message)
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

    @JvmStatic
    fun getConnectionService() = connectionStateService

    @JvmStatic
    fun getChannelsMiddleWare() = persistenceChannelsMiddleWare

    @JvmStatic
    fun getMessagesMiddleWare() = persistenceMessagesMiddleWare

    @JvmStatic
    fun getAttachmentsMiddleWare() = persistenceAttachmentsMiddleWare

    @JvmStatic
    fun getMembersMiddleWare() = persistenceMembersMiddleWare

    @JvmStatic
    fun getUserMiddleWare() = persistenceUsersMiddleWare

    @JvmStatic
    fun getSyncManager() = sceytSyncManager

    @JvmStatic
    fun getFileTransferService() = filesTransferService

    @JvmStatic
    fun addListener(key: String, listener: (success: Boolean, errorMessage: String?) -> Unit) {
        listenersMap[key] = listener
    }

    @JvmStatic
    fun clearData() {
        globalScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            preferences.clear()
            channelsCache.clear()
        }
    }

    @JvmStatic
    fun logOut(unregisterPushCallback: ((success: Boolean, errorMessage: String?) -> Unit)? = null) {
        clearData()
        WorkManager.getInstance(context).cancelAllWork()
        ClientWrapper.currentUser = null
        clientUserId = null
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