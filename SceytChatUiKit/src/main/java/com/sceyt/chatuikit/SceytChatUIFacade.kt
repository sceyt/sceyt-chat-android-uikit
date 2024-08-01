package com.sceyt.chatuikit

import android.content.Context
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.SceytDatabase
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageMarkerInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.services.SceytSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SceytChatUIFacade(
        private val context: Context,
        private val preferences: SceytSharedPreference,
        private val database: SceytDatabase,
        private val channelsCache: ChannelsCache,
        val sceytSyncManager: SceytSyncManager,
        val filesTransferService: FileTransferService,
        val channelInteractor: ChannelInteractor,
        val messageInteractor: MessageInteractor,
        val channelMemberInteractor: ChannelMemberInteractor,
        val userInteractor: UserInteractor,
        val attachmentInteractor: AttachmentInteractor,
        val messageReactionInteractor: MessageReactionInteractor,
        val messageMarkerInteractor: MessageMarkerInteractor
) : SceytKoinComponent {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var clientUserId: String? = null

    val myId
        get() = clientUserId ?: preferences.getUserId().also {
            clientUserId = it
        }

    private val _onTokenExpired: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenExpired = _onTokenExpired.asSharedFlow()

    private val _onTokenWillExpire: MutableSharedFlow<Unit> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onTokenWillExpire = _onTokenWillExpire.asSharedFlow()

    init {
        FirebaseApp.initializeApp(context)
        setListener()
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

    private fun setListener() {
        ConnectionEventsObserver.onTokenExpired.onEach {
            _onTokenExpired.tryEmit(Unit)
        }.launchIn(scope)

        ConnectionEventsObserver.onTokenWillExpire.onEach {
            _onTokenWillExpire.tryEmit(Unit)
        }.launchIn(scope)
    }


    fun clearData() {
        database.clearAllTables()
        preferences.clear()
        channelsCache.clear()
    }

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