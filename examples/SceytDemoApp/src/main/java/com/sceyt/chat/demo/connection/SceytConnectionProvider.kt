package com.sceyt.chat.demo.connection

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class SceytConnectionProvider(
    private val application: Application,
    private val preference: AppSharedPreference,
    private val chatClientConnectionInterceptor: ChatClientConnectionInterceptor,
) : CoroutineScope {
    private var initialized = false
    private var isConnecting = AtomicBoolean(false)

    companion object {
        private const val TAG = "SceytConnectionProvider"
    }

    fun init() {
        if (initialized) return
        observeToAppLifecycle()
        observeToConnectionState()

        launch {
            SceytChatUIKit.chatUIFacade.onTokenExpired.collect {
                SceytLog.i(TAG, "onTokenExpired")
                launch {
                    chatClientConnectionInterceptor.getChatToken(SceytChatUIKit.chatUIFacade.myId.toString())?.let { token ->
                        if (application.isAppOnForeground()) {
                            SceytLog.i(TAG, "onTokenExpired, will connect with new token: ${token.take(8)}")
                            SceytChatUIKit.connect(token)
                        }
                    } ?: run {
                        SceytLog.i(TAG, "connectChatClient failed because ChatClient token is null. Called in onTokenExpired")
                    }
                }
            }
        }

        launch {
            SceytChatUIKit.chatUIFacade.onTokenWillExpire.collect {
                launch {
                    SceytLog.i(TAG, "onTokenWillExpire")
                    chatClientConnectionInterceptor.getChatToken(SceytChatUIKit.chatUIFacade.myId.toString())?.let { token ->
                        SceytChatUIKit.chatUIFacade.updateToken(token, listener = { success, errorMessage ->
                            if (!success) {
                                SceytLog.e(TAG, "$TAG update chatSdk Token failed, will connect, error: $errorMessage")
                                if (application.isAppOnForeground()) {
                                    SceytLog.i(TAG, "$TAG onTokenWillExpire, will connect with new token: ${token.take(8)}")
                                    SceytChatUIKit.connect(token)
                                }
                            } else
                                SceytLog.i(TAG, "$TAG updateToken success")
                        })
                    } ?: run {
                        SceytLog.i(TAG, "$TAG connectChatClient failed because ChatClient token is null. Called in onTokenWillExpire")
                    }
                }
            }
        }

        initialized = true
    }

    fun connectChatClient(
        userId: String = preference.getString(AppSharedPreference.PREF_USER_ID) ?: "",
        onConnectStarted: ((Boolean, Exception?) -> Unit)? = null,
    ) {
        launch {
            val savedUserId = preference.getString(AppSharedPreference.PREF_USER_ID)
            if (userId.isNotBlank() && !savedUserId.isNullOrBlank() && userId != savedUserId) {
                if (ConnectionEventManager.isConnected || ConnectionEventManager.isConnecting) {
                    ChatClient.getClient().disconnect()
                }
                preference.setString(AppSharedPreference.PREF_USER_TOKEN, null)
                preference.setString(AppSharedPreference.PREF_USER_ID, userId)
            }

            if (ConnectionEventManager.connectionState == ConnectionState.Connecting) {
                SceytLog.i(TAG, "$TAG connectChatClient ignore login because ChatClient is connecting.")
                return@launch
            }

            if (ConnectionEventManager.isConnected) {
                SceytLog.i(TAG, "$TAG connectChatClient ignore login because ChatClient is connected.")
                return@launch
            }

            if (isConnecting.get()) {
                SceytLog.i(TAG, "$TAG connectChatClient ignore because started connecting flow.")
                return@launch
            }

            val sceytToken = preference.getString(AppSharedPreference.PREF_USER_TOKEN)

            if (userId.isBlank() && sceytToken.isNullOrBlank()) {
                SceytLog.i(TAG, "$TAG connectChatClient ignore login because has not userId and token.")
                return@launch
            }

            isConnecting.set(true)

            if (!sceytToken.isNullOrBlank()) {
                SceytLog.i(TAG, "$TAG saved ChatClient token is exist, trying connect with that token: ${sceytToken}.")
                SceytChatUIKit.connect(sceytToken)
                onConnectStarted?.invoke(true, null)
            } else {
                SceytLog.i(TAG, "$TAG saved ChatClient token is empty, trying to get Cat client token, userId: $userId.")
                chatClientConnectionInterceptor.getChatToken(userId)?.let { token ->
                    SceytLog.i(TAG, "$TAG connectChatClient will connect with new token: ${token.take(8)}")
                    SceytChatUIKit.connect(token)
                    onConnectStarted?.invoke(true, null)
                } ?: run {
                    SceytLog.i(TAG, "$TAG connectChatClient failed because ChatClient token is null. Called in connectChatClient")
                    onConnectStarted?.invoke(false, Exception("Couldn't get chat token, please try again."))
                }
            }
            isConnecting.set(false)
        }
    }

    private fun observeToAppLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    connectChatClient()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    SceytChatUIKit.disconnect()
                }

                else -> {}
            }
        })
    }

    private fun observeToConnectionState() {
        launch {
            ConnectionEventManager.onChangedConnectStatusFlow.collect {
                when (it.state) {
                    ConnectionState.Failed -> SceytLog.e(TAG, "${it.exception?.message}")
                    ConnectionState.Disconnected -> {
                        if (it.exception?.code == 1021) {
                            SceytLog.i(TAG, "disconnected, reason ${it.exception?.message}, clear old token because of 1021 error")
                            preference.setString(AppSharedPreference.PREF_USER_TOKEN, null)
                        } else
                            SceytLog.i(TAG, "$TAG disconnected ${it.exception?.message}")
                    }

                    else -> Unit
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}