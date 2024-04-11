package com.sceyt.chatuikit

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
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChanelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.SceytDatabase
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.pushes.SceytFirebaseMessagingDelegate
import com.sceyt.chatuikit.services.SceytSyncManager
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

object SceytKitClient : SceytKoinComponent {
    private val context: Context by inject()
    private val preferences: SceytSharedPreference by inject()
    private val database: SceytDatabase by inject()
    private val channelsCache: ChannelsCache by inject()
    private val persistenceChannelsMiddleWare by inject<ChanelInteractor>()
    private val messageInteractor by inject<MessageInteractor>()
    private val channelMemberInteractor by inject<ChannelMemberInteractor>()
    private val userInteractor by inject<UserInteractor>()
    private val attachmentInteractor by inject<AttachmentInteractor>()
    private val sceytSyncManager by inject<SceytSyncManager>()
    private val filesTransferService by inject<FileTransferService>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val listenersMap = hashMapOf<String, (success: Boolean, errorMessage: String?) -> Unit>()
    private var clientUserId: String? = null

    @JvmStatic
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
                    scope.launch {
                        ProcessLifecycleOwner.get().repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            if (ConnectionEventsObserver.isConnected)
                                userInteractor.setPresenceState(PresenceState.Online)
                        }
                    }
                    SceytFirebaseMessagingDelegate.checkNeedRegisterForPushToken()
                    scope.launch(Dispatchers.IO) {
                        messageInteractor.sendAllPendingMarkers()
                        messageInteractor.sendAllPendingMessages()
                        messageInteractor.sendAllPendingMessageStateUpdates()
                        messageInteractor.sendAllPendingReactions()
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
        }.launchIn(scope)

        ConnectionEventsObserver.onTokenExpired.onEach {
            onTokenExpired_.tryEmit(Unit)
        }.launchIn(scope)

        ConnectionEventsObserver.onTokenWillExpire.onEach {
            onTokenWillExpire_.tryEmit(Unit)
        }.launchIn(scope)
    }

    private fun notifyState(success: Boolean, errorMessage: String?) {
        listenersMap.values.forEach { listener ->
            listener.invoke(success, errorMessage)
        }
    }

    private fun getChatClient(): ChatClient? = ChatClient.getClient()

    @JvmStatic
    fun getChannelsMiddleWare() = persistenceChannelsMiddleWare

    @JvmStatic
    fun getMessagesMiddleWare() = messageInteractor

    @JvmStatic
    fun getAttachmentsMiddleWare() = attachmentInteractor

    @JvmStatic
    fun getMembersMiddleWare() = channelMemberInteractor

    @JvmStatic
    fun getUserMiddleWare() = userInteractor

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
        database.clearAllTables()
        preferences.clear()
        channelsCache.clear()
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
}