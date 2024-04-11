package com.sceyt.chat.demo.connection

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.sceyt.chat.ChatClient
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chatuikit.SceytKitClient
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver
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
        private val chatClientConnectionInterceptor: ChatClientConnectionInterceptor
) : CoroutineScope {
    private var initialized = false
    private var isConnecting = AtomicBoolean(false)

    companion object {
        const val Tag = "SceytConnectionProvider"
    }

    fun init() {
        if (initialized) return
        observeToAppLifecycle()
        observeToConnectionState()

        launch {
            SceytKitClient.onTokenExpired.collect {
                SceytLog.i(Tag, "onTokenExpired")
                launch {
                    chatClientConnectionInterceptor.getChatToken(SceytKitClient.myId.toString())?.let { token ->
                        if (application.isAppOnForeground()) {
                            SceytLog.i(Tag, "onTokenExpired, will connect with new token: ${token.take(8)}")
                            SceytKitClient.connect(token)
                        }
                    } ?: run {
                        SceytLog.i(Tag, "connectChatClient failed because ChatClient token is null. Called in onTokenExpired")
                    }
                }
            }
        }

        launch {
            SceytKitClient.onTokenWillExpire.collect {
                launch {
                    SceytLog.i(Tag, "onTokenWillExpire")
                    chatClientConnectionInterceptor.getChatToken(SceytKitClient.myId.toString())?.let { token ->
                        SceytKitClient.updateToken(token, listener = { success, errorMessage ->
                            if (!success) {
                                SceytLog.e(Tag, "$Tag update chatSdk Token failed, will connect, error: $errorMessage")
                                if (application.isAppOnForeground()) {
                                    SceytLog.i(Tag, "$Tag onTokenWillExpire, will connect with new token: ${token.take(8)}")
                                    SceytKitClient.connect(token)
                                }
                            } else
                                SceytLog.i(Tag, "$Tag updateToken success")
                        })
                    } ?: run {
                        SceytLog.i(Tag, "$Tag connectChatClient failed because ChatClient token is null. Called in onTokenWillExpire")
                    }
                }
            }
        }

        initialized = true
    }

    fun connectChatClient(userId: String = preference.getString(AppSharedPreference.PREF_USER_ID)
            ?: "") {
        launch {
            val savedUserId = preference.getString(AppSharedPreference.PREF_USER_ID)
            if (userId.isNotBlank() && !savedUserId.isNullOrBlank() && userId != savedUserId) {
                ChatClient.getClient().disconnect()
                preference.setString("", AppSharedPreference.PREF_USER_TOKEN)
            }

            if (ConnectionEventsObserver.connectionState == ConnectionState.Connecting) {
                SceytLog.i(Tag, "$Tag connectChatClient ignore login because ChatClient is connecting.")
                return@launch
            }

            if (ConnectionEventsObserver.isConnected) {
                SceytLog.i(Tag, "$Tag connectChatClient ignore login because ChatClient is connected.")
                return@launch
            }

            if (isConnecting.get()) {
                SceytLog.i(Tag, "$Tag connectChatClient ignore because started connecting flow.")
                return@launch
            }

            val sceytToken = preference.getString(AppSharedPreference.PREF_USER_TOKEN)

            if (userId.isBlank() && sceytToken.isNullOrBlank()) {
                SceytLog.i(Tag, "$Tag connectChatClient ignore login because has not userId and token.")
                return@launch
            }

            isConnecting.set(true)

            if (!sceytToken.isNullOrBlank()) {
                SceytLog.i(Tag, "$Tag saved ChatClient token is exist, trying connect with that token: ${sceytToken}.")
                SceytKitClient.connect(sceytToken)
            } else {
                SceytLog.i(Tag, "$Tag saved ChatClient token is empty, trying to get Cat client token, userId: $userId.")
                chatClientConnectionInterceptor.getChatToken(userId)?.let { token ->
                    SceytLog.i(Tag, "$Tag connectChatClient will connect with new token: ${token.take(8)}")
                    SceytKitClient.connect(token)
                } ?: run {
                    SceytLog.i(Tag, "$Tag connectChatClient failed because ChatClient token is null. Called in connectChatClient")
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
                    SceytKitClient.disconnect()
                }

                else -> {}
            }
        })
    }

    private fun observeToConnectionState() {
        launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.collect {
                when (it.state) {
                    ConnectionState.Failed -> SceytLog.e(Tag, "${it.exception?.message}")
                    ConnectionState.Disconnected -> {
                        if (it.exception?.code == 1021) {
                            SceytLog.i(Tag, "disconnected, reason ${it.exception?.message}, clear old token because of 1021 error")
                            preference.setString(AppSharedPreference.PREF_USER_TOKEN, null)
                        } else
                            SceytLog.i(Tag, "$Tag disconnected ${it.exception?.message}")
                    }

                    else -> Unit
                }
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}