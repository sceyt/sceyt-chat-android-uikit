package com.sceyt.chat.connection

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages the process-wide Chat SDK connection.
 *
 * Create one application-scoped instance and reuse it for the lifetime of the process.
 */
class SceytChatConnectionManager internal constructor(
    private val client: ChatConnectionClient,
    private val tokenResolver: ChatTokenResolver,
    private val config: ChatConnectionConfig,
    private val lifecycle: Lifecycle,
    private val scope: CoroutineScope
) {

    constructor(
        context: Context,
        tokenProvider: ChatTokenProvider,
        config: ChatConnectionConfig = ChatConnectionConfig()
    ) : this(
        client = DefaultChatConnectionClient(ChatClient.getClient()),
        tokenResolver = ChatTokenResolver(
            provider = tokenProvider,
            storage = SharedPreferencesChatTokenStorage(context),
            expirationLeewaySeconds = config.tokenExpirationLeewaySeconds
        ),
        config = config,
        lifecycle = ProcessLifecycleOwner.get().lifecycle,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    )

    private val _status = MutableStateFlow(
        ChatConnectionStatus(connectionState = client.connectionState)
    )
    val status: StateFlow<ChatConnectionStatus> = _status.asStateFlow()

    private var userId: String? = null
    private var isForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    private var tokenRequestJob: Job? = null
    private var tokenRequestUserId: String? = null
    private var tokenRequestPurpose: TokenRequestPurpose? = null
    private var tokenRequestGeneration = 0L
    private var backgroundDisconnectJob: Job? = null
    private var reconnectAfterBackgroundDisconnect = false
    private var isRecoveringFromTokenError = false

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            handleAppForegrounded()
        }

        override fun onStop(owner: LifecycleOwner) {
            handleAppBackgrounded()
        }
    }

    private val clientListener = object : ChatConnectionClient.Listener {
        override fun onConnectionStateChanged(
            state: ConnectionState,
            error: SceytException?
        ) {
            handleConnectionStateChanged(state, error)
        }

        override fun onTokenWillExpire() {
            handleTokenWillExpire()
        }

        override fun onTokenExpired() {
            handleTokenExpired()
        }
    }

    init {
        client.setListener(clientListener)
        lifecycle.addObserver(lifecycleObserver)
    }

    fun connect(userId: String) {
        val normalizedUserId = userId.trim()
        require(normalizedUserId.isNotEmpty()) { "userId must not be blank" }
        check(client.isReadyForConnection) {
            "ChatClient must be initialized before connecting"
        }

        scope.launch {
            cancelBackgroundDisconnect()
            isRecoveringFromTokenError = false

            val previousUserId = this@SceytChatConnectionManager.userId
            val isSwitchingUser = previousUserId != null && previousUserId != normalizedUserId
            val isReplacingUnmanagedConnection = previousUserId == null &&
                client.connectionState.isActive &&
                client.connectedUserId != normalizedUserId

            val shouldReplaceConnection = isSwitchingUser || isReplacingUnmanagedConnection
            val shouldReconnectAfterBackgroundDisconnect = reconnectAfterBackgroundDisconnect
            reconnectAfterBackgroundDisconnect = false
            this@SceytChatConnectionManager.userId = normalizedUserId

            _status.update { current ->
                current.copy(
                    userId = normalizedUserId,
                    connectionState = if (shouldReplaceConnection) {
                        ConnectionState.Disconnected
                    } else {
                        client.connectionState
                    },
                    error = null
                )
            }

            if (shouldReplaceConnection) {
                cancelTokenRequest()
                tokenResolver.invalidate()
                if (client.connectionState.isActive) {
                    client.disconnect()
                }
            }

            if (
                shouldReplaceConnection ||
                shouldReconnectAfterBackgroundDisconnect ||
                !client.connectionState.isActive
            ) {
                requestToken(normalizedUserId, TokenRequestPurpose.Connect)
            }
        }
    }

    suspend fun connectAndAwait(
        userId: String,
        timeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS
    ): Result<Unit> {
        require(timeoutMillis > 0L) { "timeoutMillis must be positive" }
        val normalizedUserId = userId.trim()

        return try {
            connect(normalizedUserId)
            val result = withTimeoutOrNull(timeoutMillis.milliseconds) {
                status.first { current ->
                    current.userId == normalizedUserId && current.isTerminalConnectionState()
                }
            } ?: return Result.failure(
                IllegalStateException("Connection timed out after $timeoutMillis ms")
            )

            if (result.connectionState == ConnectionState.Connected) {
                Result.success(Unit)
            } else {
                Result.failure(result.error ?: IllegalStateException("Connection failed"))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun disconnect() {
        scope.launch {
            cancelBackgroundDisconnect()
            cancelTokenRequest()
            reconnectAfterBackgroundDisconnect = false
            client.disconnect()
        }
    }

    fun logout() {
        scope.launch {
            cancelBackgroundDisconnect()
            cancelTokenRequest()
            userId = null
            reconnectAfterBackgroundDisconnect = false
            tokenResolver.invalidate()
            isRecoveringFromTokenError = false
            _status.value = ChatConnectionStatus(
                connectionState = ConnectionState.Disconnected
            )
            client.disconnect()
        }
    }

    private fun handleAppForegrounded() {
        scope.launch {
            isForeground = true
            cancelBackgroundDisconnect()

            val currentUserId = userId
            val shouldReconnectAfterBackgroundDisconnect = reconnectAfterBackgroundDisconnect
            reconnectAfterBackgroundDisconnect = false
            if (
                config.reconnectOnForeground &&
                currentUserId != null &&
                (shouldReconnectAfterBackgroundDisconnect || !client.connectionState.isActive)
            ) {
                requestToken(currentUserId, TokenRequestPurpose.Connect)
            }
        }
    }

    private fun handleAppBackgrounded() {
        scope.launch {
            isForeground = false
            scheduleBackgroundDisconnect()
        }
    }

    private fun handleConnectionStateChanged(
        state: ConnectionState,
        error: SceytException?
    ) {
        scope.launch {
            _status.update { current ->
                current.copy(
                    connectionState = state,
                    error = error
                )
            }

            when {
                state == ConnectionState.Connected -> isRecoveringFromTokenError = false
                isTokenError(state, error) -> recoverFromTokenError()
            }
        }
    }

    private fun handleTokenWillExpire() {
        scope.launch {
            if (!shouldRefreshToken()) return@launch

            userId?.let { currentUserId ->
                requestToken(
                    currentUserId,
                    TokenRequestPurpose.Update,
                    forceRefresh = true
                )
            }
        }
    }

    private fun handleTokenExpired() {
        scope.launch {
            if (!shouldRefreshToken()) return@launch

            userId?.let { currentUserId ->
                requestToken(
                    currentUserId,
                    TokenRequestPurpose.Connect,
                    forceRefresh = true
                )
            }
        }
    }

    private fun requestToken(
        userId: String,
        purpose: TokenRequestPurpose,
        forceRefresh: Boolean = false
    ) {
        if (tokenRequestJob?.isActive == true && tokenRequestUserId == userId) {
            tokenRequestPurpose = tokenRequestPurpose.merge(purpose)
            return
        }

        cancelTokenRequest()
        val generation = ++tokenRequestGeneration
        tokenRequestUserId = userId
        tokenRequestPurpose = purpose
        _status.update { it.copy(isFetchingToken = true, error = null) }

        tokenRequestJob = scope.launch {
            try {
                val token = tokenResolver.resolve(userId, forceRefresh)
                currentCoroutineContext().ensureActive()

                if (token.isNullOrBlank()) {
                    setTokenError(userId, IllegalStateException("Token provider returned no token"))
                    return@launch
                }

                if (!isCurrentRequest(generation, userId)) {
                    return@launch
                }

                when (tokenRequestPurpose) {
                    TokenRequestPurpose.Connect -> connectWithToken(userId, token)
                    TokenRequestPurpose.Update -> updateToken(userId, token)
                    null -> Unit
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                setTokenError(userId, error)
            } finally {
                if (generation == tokenRequestGeneration) {
                    tokenRequestJob = null
                    tokenRequestUserId = null
                    tokenRequestPurpose = null
                    _status.update { it.copy(isFetchingToken = false) }
                }
            }
        }
    }

    private fun recoverFromTokenError() {
        tokenResolver.invalidate()
        val currentUserId = userId ?: return
        if (isRecoveringFromTokenError) return

        isRecoveringFromTokenError = true
        requestToken(
            currentUserId,
            TokenRequestPurpose.Connect,
            forceRefresh = true
        )
    }

    private fun isTokenError(
        state: ConnectionState,
        error: SceytException?
    ): Boolean =
        state.isDisconnected && error?.code in config.tokenRefreshErrorCodes

    private fun shouldRefreshToken(): Boolean =
        isForeground ||
            config.backgroundConnectionPolicy == BackgroundConnectionPolicy.KeepConnected ||
            !reconnectAfterBackgroundDisconnect

    private fun ChatConnectionStatus.isTerminalConnectionState(): Boolean {
        if (connectionState == ConnectionState.Connected) return true
        if (isFetchingToken) return false

        val isRecoverableTokenError = error is SceytException &&
            error.code in config.tokenRefreshErrorCodes
        return !isRecoverableTokenError &&
            (error != null || connectionState == ConnectionState.Failed)
    }

    private fun connectWithToken(userId: String, token: String) {
        if (this.userId != userId) return

        try {
            client.connect(token)
        } catch (error: Exception) {
            setTokenError(userId, error)
        }
    }

    private suspend fun updateToken(userId: String, token: String) {
        val result = try {
            client.updateToken(token)
        } catch (error: Exception) {
            Result.failure(error)
        }

        if (result.isFailure && isCurrentUser(userId)) {
            connectWithToken(userId, token)
        }
    }

    private fun scheduleBackgroundDisconnect() {
        val policy = config.backgroundConnectionPolicy as? BackgroundConnectionPolicy.Disconnect
            ?: return

        cancelBackgroundDisconnect()
        backgroundDisconnectJob = scope.launch {
            delay(policy.delayMillis.milliseconds)
            if (!isForeground && userId != null) {
                cancelTokenRequest()
                reconnectAfterBackgroundDisconnect = true
                client.disconnect()
            }
        }
    }

    private fun cancelBackgroundDisconnect() {
        backgroundDisconnectJob?.cancel()
        backgroundDisconnectJob = null
    }

    private fun cancelTokenRequest() {
        tokenRequestGeneration++
        tokenRequestJob?.cancel()
        tokenRequestJob = null
        tokenRequestUserId = null
        tokenRequestPurpose = null
        _status.update { it.copy(isFetchingToken = false) }
    }

    private fun setTokenError(userId: String, error: Throwable) {
        if (isCurrentUser(userId)) {
            _status.update { it.copy(error = error) }
        }
    }

    private fun isCurrentRequest(generation: Long, userId: String): Boolean =
        generation == tokenRequestGeneration && isCurrentUser(userId)

    private fun isCurrentUser(userId: String): Boolean = this.userId == userId

    private enum class TokenRequestPurpose {
        Connect,
        Update
    }

    private fun TokenRequestPurpose?.merge(other: TokenRequestPurpose): TokenRequestPurpose = when {
        this == TokenRequestPurpose.Connect -> this
        other == TokenRequestPurpose.Connect -> other
        else -> TokenRequestPurpose.Update
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}

private val ConnectionState.isDisconnected: Boolean
    get() = this == ConnectionState.Disconnected || this == ConnectionState.Failed

private val ConnectionState.isActive: Boolean
    get() = when (this) {
        ConnectionState.Connecting,
        ConnectionState.Reconnecting,
        ConnectionState.Connected -> true

        ConnectionState.Disconnected,
        ConnectionState.Failed -> false
    }
