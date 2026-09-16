package com.sceyt.chat.connection

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SceytChatConnectionManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun connectFetchesTokenAndStartsConnection() = runTest {
        val fixture = createFixture(tokens = listOf("token-1"))

        fixture.manager.connect(" alice ")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("token-1")
        assertThat(fixture.manager.status.value.userId).isEqualTo("alice")
        assertThat(fixture.manager.status.value.isFetchingToken).isFalse()

        fixture.close()
    }

    @Test
    fun connectAndAwaitReturnsAfterRequestedUserConnects() = runTest {
        val fixture = createFixture(tokens = listOf("token-1"))

        val result = async {
            fixture.manager.connectAndAwait("alice", timeoutMillis = 1_000L)
        }
        runCurrent()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        advanceUntilIdle()

        assertThat(result.await().isSuccess).isTrue()
        assertThat(fixture.manager.status.value.userId).isEqualTo("alice")

        fixture.close()
    }

    @Test
    fun connectAndAwaitReturnsTokenProviderFailure() = runTest {
        val failure = IllegalStateException("token request failed")
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider { throw failure }
        )

        val result = async {
            fixture.manager.connectAndAwait("alice", timeoutMillis = 1_000L)
        }
        advanceUntilIdle()

        assertThat(result.await().exceptionOrNull()).isSameInstanceAs(failure)

        fixture.close()
    }

    @Test
    fun blankUserIdIsRejected() = runTest {
        val fixture = createFixture()

        assertThrows(IllegalArgumentException::class.java) {
            fixture.manager.connect("  ")
        }

        fixture.close()
    }

    @Test
    fun connectBeforeSdkInitializationIsRejected() = runTest {
        val fixture = createFixture(isReadyForConnection = false)

        val error = assertThrows(IllegalStateException::class.java) {
            fixture.manager.connect("alice")
        }

        assertThat(error).hasMessageThat()
            .isEqualTo("ChatClient must be initialized before connecting")
        assertThat(fixture.client.connectedTokens).isEmpty()

        fixture.close()
    }

    @Test
    fun connectDoesNothingWhenSameUserIsAlreadyConnected() = runTest {
        var tokenRequestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                tokenRequestCount++
                "token-1"
            }
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        advanceUntilIdle()

        fixture.manager.connect("alice")
        advanceUntilIdle()

        assertThat(tokenRequestCount).isEqualTo(1)
        assertThat(fixture.client.disconnectCount).isEqualTo(0)
        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun connectReplacesActiveSdkConnectionForDifferentUser() = runTest {
        val fixture = createFixture(
            tokens = listOf("bob-token"),
            initialConnectionState = ConnectionState.Connected,
            connectedUserId = "alice"
        )

        fixture.manager.connect("bob")
        advanceUntilIdle()

        assertThat(fixture.client.disconnectCount).isEqualTo(1)
        assertThat(fixture.client.connectedTokens).containsExactly("bob-token")
        assertThat(fixture.manager.status.value.userId).isEqualTo("bob")

        fixture.close()
    }

    @Test
    fun connectReusesActiveSdkConnectionForSameUser() = runTest {
        var tokenRequestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                tokenRequestCount++
                "token-1"
            },
            initialConnectionState = ConnectionState.Connected,
            connectedUserId = "alice"
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()

        assertThat(tokenRequestCount).isEqualTo(0)
        assertThat(fixture.client.disconnectCount).isEqualTo(0)
        assertThat(fixture.manager.status.value.userId).isEqualTo("alice")

        fixture.close()
    }

    @Test
    fun disconnectCancelsPendingTokenRequest() = runTest {
        val token = CompletableDeferred<String?>()
        val fixture = createFixture(tokenProvider = ChatTokenProvider { token.await() })

        fixture.manager.connect("alice")
        runCurrent()

        assertThat(fixture.manager.status.value.isFetchingToken).isTrue()

        fixture.manager.disconnect()
        advanceUntilIdle()
        token.complete("stale-token")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).isEmpty()
        assertThat(fixture.client.disconnectCount).isEqualTo(1)
        assertThat(fixture.manager.status.value.isFetchingToken).isFalse()

        fixture.close()
    }

    @Test
    fun repeatedConnectCoalescesTokenRequest() = runTest {
        val token = CompletableDeferred<String?>()
        var requestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                requestCount++
                token.await()
            }
        )

        fixture.manager.connect("alice")
        runCurrent()
        fixture.manager.connect("alice")
        runCurrent()

        assertThat(requestCount).isEqualTo(1)

        token.complete("token-1")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun switchingUserIgnoresPreviousTokenRequest() = runTest {
        val aliceToken = CompletableDeferred<String?>()
        val bobToken = CompletableDeferred<String?>()
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider { userId ->
                when (userId) {
                    "alice" -> aliceToken.await()
                    "bob" -> bobToken.await()
                    else -> null
                }
            }
        )

        fixture.manager.connect("alice")
        runCurrent()
        fixture.manager.connect("bob")
        runCurrent()

        aliceToken.complete("alice-token")
        bobToken.complete("bob-token")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("bob-token")
        assertThat(fixture.manager.status.value.userId).isEqualTo("bob")

        fixture.close()
    }

    @Test
    fun switchingUserDisconnectsActiveClient() = runTest {
        val fixture = createFixture(tokens = listOf("alice-token", "bob-token"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        advanceUntilIdle()

        fixture.manager.connect("bob")
        advanceUntilIdle()

        assertThat(fixture.client.disconnectCount).isEqualTo(1)
        assertThat(fixture.client.connectedTokens)
            .containsExactly("alice-token", "bob-token")
            .inOrder()
        assertThat(fixture.manager.status.value.userId).isEqualTo("bob")

        fixture.close()
    }

    @Test
    fun tokenWillExpireUpdatesToken() = runTest {
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.emitTokenWillExpire()
        advanceUntilIdle()

        assertThat(fixture.client.updatedTokens).containsExactly("token-2")
        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun tokenUpdateRequestDoesNotReplacePendingConnect() = runTest {
        val token = CompletableDeferred<String?>()
        val fixture = createFixture(tokenProvider = ChatTokenProvider { token.await() })

        fixture.manager.connect("alice")
        runCurrent()
        fixture.client.emitTokenWillExpire()
        runCurrent()

        token.complete("token-1")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("token-1")
        assertThat(fixture.client.updatedTokens).isEmpty()

        fixture.close()
    }

    @Test
    fun tokenExpiredPromotesPendingUpdateToConnect() = runTest {
        val refreshedToken = CompletableDeferred<String?>()
        var tokenRequestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                tokenRequestCount++
                if (tokenRequestCount == 1) "token-1" else refreshedToken.await()
            }
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.emitTokenWillExpire()
        runCurrent()

        fixture.client.emitTokenExpired()
        runCurrent()
        refreshedToken.complete("token-2")
        advanceUntilIdle()

        assertThat(tokenRequestCount).isEqualTo(2)
        assertThat(fixture.client.updatedTokens).isEmpty()
        assertThat(fixture.client.connectedTokens)
            .containsExactly("token-1", "token-2")
            .inOrder()

        fixture.close()
    }

    @Test
    fun repeatedTokenUpdateEventsSharePendingRequest() = runTest {
        val refreshedToken = CompletableDeferred<String?>()
        var tokenRequestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                tokenRequestCount++
                if (tokenRequestCount == 1) "token-1" else refreshedToken.await()
            }
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.emitTokenWillExpire()
        runCurrent()
        fixture.client.emitTokenWillExpire()
        runCurrent()

        refreshedToken.complete("token-2")
        advanceUntilIdle()

        assertThat(tokenRequestCount).isEqualTo(2)
        assertThat(fixture.client.updatedTokens).containsExactly("token-2")

        fixture.close()
    }

    @Test
    fun failedTokenUpdateReconnectsWithFreshToken() = runTest {
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))
        fixture.client.updateTokenResult = Result.failure(IllegalStateException("update failed"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.emitTokenWillExpire()
        advanceUntilIdle()

        assertThat(fixture.client.updatedTokens).containsExactly("token-2")
        assertThat(fixture.client.connectedTokens).containsExactly("token-1", "token-2").inOrder()

        fixture.close()
    }

    @Test
    fun thrownTokenUpdateErrorReconnectsWithFreshToken() = runTest {
        val failure = IllegalStateException("update failed")
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))
        fixture.client.updateTokenError = failure

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.emitTokenWillExpire()
        advanceUntilIdle()

        assertThat(fixture.client.updatedTokens).containsExactly("token-2")
        assertThat(fixture.client.connectedTokens)
            .containsExactly("token-1", "token-2")
            .inOrder()

        fixture.close()
    }

    @Test
    fun tokenExpiredReconnectsWithFreshToken() = runTest {
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.emitTokenExpired()
        advanceUntilIdle()

        assertThat(fixture.client.updatedTokens).isEmpty()
        assertThat(fixture.client.connectedTokens).containsExactly("token-1", "token-2").inOrder()

        fixture.close()
    }

    @Test
    fun tokenExpiredDoesNotReconnectAfterBackgroundPolicyDisconnects() = runTest {
        val fixture = createFixture(
            tokens = listOf("token-1", "token-2"),
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect()
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        advanceUntilIdle()

        fixture.client.emitTokenExpired()
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun tokenProviderFailureIsExposedWithoutConnecting() = runTest {
        val failure = IllegalStateException("token request failed")
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider { throw failure }
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).isEmpty()
        assertThat(fixture.manager.status.value.error).isSameInstanceAs(failure)
        assertThat(fixture.manager.status.value.isFetchingToken).isFalse()

        fixture.close()
    }

    @Test
    fun nullOrBlankTokenIsExposedWithoutConnecting() = runTest {
        listOf(null, "", "  ").forEach { invalidToken ->
            val fixture = createFixture(
                tokenProvider = ChatTokenProvider { invalidToken }
            )

            fixture.manager.connect("alice")
            advanceUntilIdle()

            assertThat(fixture.client.connectedTokens).isEmpty()
            assertThat(fixture.manager.status.value.error)
                .hasMessageThat()
                .isEqualTo("Token provider returned no token")
            assertThat(fixture.manager.status.value.isFetchingToken).isFalse()

            fixture.close()
        }
    }

    @Test
    fun connectFailureIsExposed() = runTest {
        val failure = IllegalStateException("connect failed")
        val fixture = createFixture(tokens = listOf("token-1"))
        fixture.client.connectError = failure

        fixture.manager.connect("alice")
        advanceUntilIdle()

        assertThat(fixture.client.connectAttempts).containsExactly("token-1")
        assertThat(fixture.client.connectedTokens).isEmpty()
        assertThat(fixture.manager.status.value.error).isSameInstanceAs(failure)

        fixture.close()
    }

    @Test
    fun connectionStateAndErrorAreExposed() = runTest {
        val fixture = createFixture()
        val failure = SceytException(500, "connection failed")

        fixture.client.emitConnectionState(ConnectionState.Failed, failure)
        advanceUntilIdle()

        assertThat(fixture.manager.status.value.connectionState)
            .isEqualTo(ConnectionState.Failed)
        assertThat(fixture.manager.status.value.error).isSameInstanceAs(failure)

        fixture.close()
    }

    @Test
    fun tokenConnectionErrorFetchesFreshTokenAndReconnects() = runTest {
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(
            ConnectionState.Disconnected,
            SceytException(ChatConnectionConfig.TOKEN_EXPIRED_ERROR_CODE, "token expired")
        )
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens)
            .containsExactly("token-1", "token-2")
            .inOrder()

        fixture.close()
    }

    @Test
    fun nonTokenConnectionErrorDoesNotReconnect() = runTest {
        val fixture = createFixture(tokens = listOf("token-1"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(
            ConnectionState.Failed,
            SceytException(500, "connection failed")
        )
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun tokenConnectionErrorRetriesOnlyOnceUntilConnected() = runTest {
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))
        val tokenError = SceytException(
            ChatConnectionConfig.TOKEN_EXPIRED_ERROR_CODE,
            "token expired"
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Disconnected, tokenError)
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Disconnected, tokenError)
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens)
            .containsExactly("token-1", "token-2")
            .inOrder()

        fixture.close()
    }

    @Test
    fun tokenEventsWithoutUserDoNotRequestToken() = runTest {
        var tokenRequestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                tokenRequestCount++
                "token-1"
            }
        )

        fixture.client.emitTokenWillExpire()
        fixture.client.emitTokenExpired()
        advanceUntilIdle()

        assertThat(tokenRequestCount).isEqualTo(0)
        assertThat(fixture.client.connectedTokens).isEmpty()
        assertThat(fixture.client.updatedTokens).isEmpty()

        fixture.close()
    }

    @Test
    fun disconnectInBackgroundUsesConfiguredDelayAndReconnectsOnForeground() = runTest {
        val fixture = createFixture(
            tokens = listOf("token-1", "token-2"),
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect(
                    delayMillis = 1_000L
                )
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        runCurrent()

        advanceTimeBy(999L.milliseconds)
        runCurrent()
        assertThat(fixture.client.disconnectCount).isEqualTo(0)

        advanceTimeBy(1L.milliseconds)
        runCurrent()
        assertThat(fixture.client.disconnectCount).isEqualTo(1)

        fixture.lifecycleOwner.start()
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens).containsExactly("token-1", "token-2").inOrder()

        fixture.close()
    }

    @Test
    fun foregroundReconnectsWhileBackgroundDisconnectIsStillPending() = runTest {
        val fixture = createFixture(
            tokens = listOf("token-1", "token-2"),
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect()
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.client.disconnectChangesStateImmediately = false

        fixture.lifecycleOwner.stop()
        advanceUntilIdle()
        assertThat(fixture.client.connectionState).isEqualTo(ConnectionState.Connected)

        fixture.lifecycleOwner.start()
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens)
            .containsExactly("token-1", "token-2")
            .inOrder()

        fixture.close()
    }

    @Test
    fun reconnectOnForegroundReusesPersistedJwt() = runTest {
        val token = jwt(expirationEpochSeconds = NOW_EPOCH_SECONDS + 3_600L)
        var tokenRequestCount = 0
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider {
                tokenRequestCount++
                token
            },
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect()
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        advanceUntilIdle()
        fixture.lifecycleOwner.start()
        advanceUntilIdle()

        assertThat(tokenRequestCount).isEqualTo(1)
        assertThat(fixture.client.connectedTokens).containsExactly(token, token)

        fixture.close()
    }

    @Test
    fun returningToForegroundCancelsPendingBackgroundDisconnect() = runTest {
        val fixture = createFixture(
            tokens = listOf("token-1"),
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect(
                    delayMillis = 1_000L
                )
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        runCurrent()
        advanceTimeBy(999L.milliseconds)
        runCurrent()

        fixture.lifecycleOwner.start()
        runCurrent()
        advanceTimeBy(1L.milliseconds)
        runCurrent()

        assertThat(fixture.client.disconnectCount).isEqualTo(0)
        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun reconnectOnForegroundCanBeDisabled() = runTest {
        val fixture = createFixture(
            tokens = listOf("token-1"),
            config = ChatConnectionConfig(
                reconnectOnForeground = false,
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect()
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        advanceUntilIdle()
        fixture.lifecycleOwner.start()
        advanceUntilIdle()

        assertThat(fixture.client.disconnectCount).isEqualTo(1)
        assertThat(fixture.client.connectedTokens).containsExactly("token-1")

        fixture.close()
    }

    @Test
    fun backgroundDisconnectCancelsPendingTokenRequest() = runTest {
        val token = CompletableDeferred<String?>()
        val fixture = createFixture(
            tokenProvider = ChatTokenProvider { token.await() },
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect()
            )
        )

        fixture.manager.connect("alice")
        runCurrent()
        fixture.lifecycleOwner.stop()
        runCurrent()

        token.complete("stale-token")
        advanceUntilIdle()

        assertThat(fixture.client.disconnectCount).isEqualTo(1)
        assertThat(fixture.client.connectedTokens).isEmpty()
        assertThat(fixture.manager.status.value.isFetchingToken).isFalse()

        fixture.close()
    }

    @Test
    fun explicitConnectWorksInBackgroundAfterPolicyDisconnect() = runTest {
        val fixture = createFixture(
            tokens = listOf("token-1", "token-2"),
            config = ChatConnectionConfig(
                backgroundConnectionPolicy = BackgroundConnectionPolicy.Disconnect()
            )
        )

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        advanceUntilIdle()

        assertThat(fixture.client.disconnectCount).isEqualTo(1)

        fixture.manager.connect("alice")
        advanceUntilIdle()

        assertThat(fixture.client.connectedTokens)
            .containsExactly("token-1", "token-2")
            .inOrder()

        fixture.close()
    }

    @Test
    fun defaultBackgroundPolicyKeepsConnection() = runTest {
        val fixture = createFixture(tokens = listOf("token-1"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.client.emitConnectionState(ConnectionState.Connected)
        fixture.lifecycleOwner.stop()
        advanceUntilIdle()

        assertThat(fixture.client.disconnectCount).isEqualTo(0)

        fixture.close()
    }

    @Test
    fun logoutClearsUserAndPreventsForegroundReconnect() = runTest {
        val fixture = createFixture(tokens = listOf("token-1", "token-2"))

        fixture.manager.connect("alice")
        advanceUntilIdle()
        fixture.manager.logout()
        advanceUntilIdle()
        fixture.lifecycleOwner.stop()
        fixture.lifecycleOwner.start()
        advanceUntilIdle()

        assertThat(fixture.manager.status.value.userId).isNull()
        assertThat(fixture.client.connectedTokens).containsExactly("token-1")
        assertThat(fixture.client.disconnectCount).isEqualTo(1)

        fixture.close()
    }

    private fun TestScope.createFixture(
        tokens: List<String?> = emptyList(),
        tokenProvider: ChatTokenProvider = queueTokenProvider(tokens),
        config: ChatConnectionConfig = ChatConnectionConfig(),
        initialConnectionState: ConnectionState = ConnectionState.Disconnected,
        connectedUserId: String? = null,
        isReadyForConnection: Boolean = true
    ): Fixture {
        val lifecycleOwner = TestLifecycleOwner().apply { start() }
        val client = FakeChatConnectionClient(
            initialConnectionState = initialConnectionState,
            connectedUserId = connectedUserId,
            isReadyForConnection = isReadyForConnection
        )
        val managerScope = CoroutineScope(
            SupervisorJob() + StandardTestDispatcher(testScheduler)
        )
        val manager = SceytChatConnectionManager(
            client = client,
            tokenResolver = ChatTokenResolver(
                provider = tokenProvider,
                storage = FakeChatTokenStorage(),
                expirationLeewaySeconds = config.tokenExpirationLeewaySeconds,
                currentTimeMillis = { NOW_EPOCH_SECONDS * 1_000L }
            ),
            config = config,
            lifecycle = lifecycleOwner.lifecycle,
            scope = managerScope
        )
        return Fixture(manager, client, lifecycleOwner, managerScope)
    }

    private fun queueTokenProvider(tokens: List<String?>): ChatTokenProvider {
        val iterator = tokens.iterator()
        return ChatTokenProvider { iterator.next() }
    }

    private data class Fixture(
        val manager: SceytChatConnectionManager,
        val client: FakeChatConnectionClient,
        val lifecycleOwner: TestLifecycleOwner,
        val managerScope: CoroutineScope
    ) {
        fun close() {
            managerScope.cancel()
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry

        fun start() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun stop() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    private class FakeChatConnectionClient(
        initialConnectionState: ConnectionState,
        override var connectedUserId: String?,
        override var isReadyForConnection: Boolean
    ) : ChatConnectionClient {
        override var connectionState: ConnectionState = initialConnectionState
            private set

        val connectedTokens = mutableListOf<String>()
        val connectAttempts = mutableListOf<String>()
        val updatedTokens = mutableListOf<String>()
        var disconnectCount = 0
        var disconnectChangesStateImmediately = true
        var connectError: Exception? = null
        var updateTokenError: Exception? = null
        var updateTokenResult: Result<Unit> = Result.success(Unit)

        private var listener: ChatConnectionClient.Listener? = null

        override fun setListener(listener: ChatConnectionClient.Listener) {
            this.listener = listener
        }

        override fun connect(token: String) {
            connectAttempts += token
            connectError?.let { throw it }
            connectedTokens += token
            connectionState = ConnectionState.Connecting
        }

        override fun disconnect() {
            disconnectCount++
            if (disconnectChangesStateImmediately) {
                emitConnectionState(ConnectionState.Disconnected)
            }
        }

        override suspend fun updateToken(token: String): Result<Unit> {
            updatedTokens += token
            updateTokenError?.let { throw it }
            return updateTokenResult
        }

        fun emitConnectionState(
            state: ConnectionState,
            error: SceytException? = null
        ) {
            connectionState = state
            listener?.onConnectionStateChanged(state, error)
        }

        fun emitTokenWillExpire() {
            listener?.onTokenWillExpire()
        }

        fun emitTokenExpired() {
            listener?.onTokenExpired()
        }
    }

    private class FakeChatTokenStorage : ChatTokenStorage {
        private var userId: String? = null
        private var token: String? = null

        override fun get(userId: String): String? = token.takeIf { this.userId == userId }

        override fun save(userId: String, token: String) {
            this.userId = userId
            this.token = token
        }

        override fun clear() {
            userId = null
            token = null
        }
    }

    @Suppress("SameParameterValue")
    private fun jwt(expirationEpochSeconds: Long): String {
        val payload = kotlin.io.encoding.Base64.UrlSafe
            .withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT)
            .encode("{\"exp\":$expirationEpochSeconds}".toByteArray())
        return "header.$payload.signature"
    }

    private companion object {
        const val NOW_EPOCH_SECONDS = 1_800_000_000L
    }
}
