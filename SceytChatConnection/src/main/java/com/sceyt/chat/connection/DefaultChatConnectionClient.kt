package com.sceyt.chat.connection

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_listeners.ClientListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class DefaultChatConnectionClient(
    private val chatClient: ChatClient
) : ChatConnectionClient {
    override val isReadyForConnection: Boolean
        get() = ChatClient.getAppId() != null

    override val connectionState: ConnectionState
        get() = if (ChatClient.isInitialized()) {
            chatClient.connectionState()
        } else {
            ConnectionState.Disconnected
        }

    override val connectedUserId: String?
        get() = chatClient.user?.id

    override fun setListener(listener: ChatConnectionClient.Listener) {
        chatClient.addClientListener(LISTENER_KEY, object : ClientListener {
            override fun onConnectionStateChanged(
                state: ConnectionState?,
                exception: SceytException?
            ) {
                state?.let { listener.onConnectionStateChanged(it, exception) }
            }

            override fun onTokenWillExpire(expireTime: Long) {
                listener.onTokenWillExpire()
            }

            override fun onTokenExpired() {
                listener.onTokenExpired()
            }
        })
    }

    override fun connect(token: String) {
        chatClient.connect(token)
    }

    override fun disconnect() {
        chatClient.disconnect()
    }

    override suspend fun updateToken(token: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            ChatClient.updateToken(token, object : ActionCallback {
                override fun onSuccess() {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }

                override fun onError(error: SceytException?) {
                    if (continuation.isActive) {
                        continuation.resume(
                            Result.failure(error ?: IllegalStateException("Token update failed"))
                        )
                    }
                }
            })
        }

    private companion object {
        const val LISTENER_KEY = "SceytChatConnectionManager"
    }
}
