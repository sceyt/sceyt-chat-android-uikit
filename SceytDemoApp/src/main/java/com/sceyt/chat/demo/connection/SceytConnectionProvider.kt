package com.sceyt.chat.demo.connection

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.extensions.isAppOnForeground
import com.sceyt.sceytchatuikit.logger.SceytLog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(DelicateCoroutinesApi::class)
class SceytConnectionProvider(
        private val application: Application,
        private val preference: AppSharedPreference,
        private val chatClientConnectionInterceptor: ChatClientConnectionInterceptor
) {
    private var initialized = false
    private val scope = GlobalScope

    @Volatile
    private var isConnecting = AtomicBoolean(false)

    companion object {
        const val Tag = "SceytConnectionProvider"
    }

    fun init() {
        if (initialized) return
        observeToAppLifecycle()
        observeToConnectionState()

        scope.launch {
            SceytKitClient.onTokenExpired.collect {
                SceytLog.i(Tag, "onTokenExpired")
                launch {
                    chatClientConnectionInterceptor.getChatToken(SceytKitClient.myId.toString())?.let { token ->
                        if (application.isAppOnForeground()) {
                            SceytLog.i(Tag, "onTokenExpired, will connect with new token: ${token.take(8)}")
                            SceytKitClient.connect(token, SceytKitClient.myId.toString())
                        }
                    } ?: run {
                        SceytLog.i(Tag, "connectChatClient failed because ChatClient token is null. Called in onTokenExpired")
                    }
                }
            }
        }

        scope.launch {
            SceytKitClient.onTokenWillExpire.collect {
                launch {
                    SceytLog.i(Tag, "onTokenWillExpire")
                    chatClientConnectionInterceptor.getChatToken(SceytKitClient.myId.toString())?.let { token ->
                        SceytKitClient.updateToken(token, listener = { success, errorMessage ->
                            if (!success) {
                                SceytLog.e(Tag, "$Tag update chatSdk Token failed, will connect, error: $errorMessage")
                                if (application.isAppOnForeground()) {
                                    SceytLog.i(Tag, "$Tag onTokenWillExpire, will connect with new token: ${token.take(8)}")
                                    SceytKitClient.connect(token, SceytKitClient.myId.toString())
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

    fun connectChatClient() {
        scope.launch {
            val userId = preference.getUserId()
            val isLoggedIn = !userId.isNullOrBlank()

            if (!isLoggedIn) {
                SceytLog.i(Tag, "$Tag connectChatClient ignore login because user is not logged in.")
                return@launch
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

            isConnecting.set(true)

            val sceytToken = preference.getToken()

            if (!sceytToken.isNullOrBlank()) {
                SceytLog.i(Tag, "$Tag saved ChatClient token is exist, trying connect with that token: ${sceytToken}.")
                SceytKitClient.connect(sceytToken, userId.toString())
            } else {
                SceytLog.i(Tag, "$Tag saved ChatClient token is empty, trying to get Cat client token.")
                chatClientConnectionInterceptor.getChatToken(userId.toString())?.let { token ->
                    SceytLog.i(Tag, "$Tag connectChatClient will connect with new token: ${token.take(8)}")
                    SceytKitClient.connect(token, userId.toString())
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
        scope.launch {
            ConnectionEventsObserver.onChangedConnectStatusFlow.collect {
                if (it.state == ConnectionState.Failed || (it.state == ConnectionState.Disconnected && it.exception != null)) {
                    SceytLog.e(Tag, "observeToConnectionState state is ${it.state} exception: ${it.exception?.message}")
                    preference.setToken(null)
                    chatClientConnectionInterceptor.getChatToken(SceytKitClient.myId.toString())?.let { token ->
                        SceytLog.i(Tag, "$Tag connectChatClient will connect with new token: $token")
                        SceytKitClient.connect(token, SceytKitClient.myId.toString())
                    } ?: run {
                        SceytLog.i(Tag, "$Tag connectChatClient failed because ChatClient token is null. Called in observeToConnectionState")
                    }
                }
            }
        }
    }
}