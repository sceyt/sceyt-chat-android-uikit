package com.sceyt.chat.connection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.io.encoding.Base64

class ChatTokenResolverTest {

    @Test
    fun validJwtIsReusedAcrossResolverInstances() = runTest {
        val token = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val storage = FakeChatTokenStorage()
        var requestCount = 0
        val provider = ChatTokenProvider {
            requestCount++
            Result.success(token)
        }

        val firstResolver = resolver(provider, storage)
        val secondResolver = resolver(provider, storage)

        assertThat(firstResolver.resolve("alice").getOrThrow()).isEqualTo(token)
        assertThat(secondResolver.resolve("alice").getOrThrow()).isEqualTo(token)
        assertThat(requestCount).isEqualTo(1)
    }

    @Test
    fun expiredStoredJwtIsReplaced() = runTest {
        val expiredToken = jwt(NOW_EPOCH_SECONDS - 1L)
        val freshToken = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val storage = FakeChatTokenStorage().apply {
            save("alice", expiredToken)
        }
        val resolver = resolver({ Result.success(freshToken) }, storage)

        assertThat(resolver.resolve("alice").getOrThrow()).isEqualTo(freshToken)
        assertThat(storage.get("alice")).isEqualTo(freshToken)
    }

    @Test
    fun forceRefreshReplacesReusableJwt() = runTest {
        val firstToken = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val secondToken = jwt(NOW_EPOCH_SECONDS + 7_200L)
        val tokens = listOf(firstToken, secondToken).iterator()
        val storage = FakeChatTokenStorage()
        val resolver = resolver({ Result.success(tokens.next()) }, storage)

        assertThat(resolver.resolve("alice").getOrThrow()).isEqualTo(firstToken)
        assertThat(resolver.resolve("alice", forceRefresh = true).getOrThrow()).isEqualTo(secondToken)
        assertThat(storage.get("alice")).isEqualTo(secondToken)
    }

    @Test
    fun newlyFetchedJwtInsideExpiryLeewayIsRejected() = runTest {
        val resolver = resolver(
            { Result.success(jwt(NOW_EPOCH_SECONDS + 30L)) },
            FakeChatTokenStorage()
        )

        val error = resolver.resolve("alice").exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Token is expired or expires too soon")
    }

    @Test
    fun customExpiryLeewayIsApplied() = runTest {
        val token = jwt(NOW_EPOCH_SECONDS + 30L)
        val resolver = resolver(
            provider = { Result.success(token) },
            storage = FakeChatTokenStorage(),
            expirationLeewaySeconds = 10L
        )

        assertThat(resolver.resolve("alice").getOrThrow()).isEqualTo(token)
    }

    @Test
    fun opaqueTokenIsUsedButNotPersisted() = runTest {
        val storage = FakeChatTokenStorage()
        var requestCount = 0
        val resolver = resolver(
            {
                requestCount++
                Result.success("opaque-token")
            },
            storage
        )

        assertThat(resolver.resolve("alice").getOrThrow()).isEqualTo("opaque-token")
        assertThat(resolver.resolve("alice").getOrThrow()).isEqualTo("opaque-token")
        assertThat(requestCount).isEqualTo(2)
        assertThat(storage.get("alice")).isNull()
    }

    @Test
    fun tokenStoredForAnotherUserIsNotReused() = runTest {
        val aliceToken = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val bobToken = jwt(NOW_EPOCH_SECONDS + 7_200L)
        val storage = FakeChatTokenStorage().apply {
            save("alice", aliceToken)
        }
        var requestedUserId: String? = null
        val resolver = resolver(
            { userId ->
                requestedUserId = userId
                Result.success(bobToken)
            },
            storage
        )

        assertThat(resolver.resolve("bob").getOrThrow()).isEqualTo(bobToken)
        assertThat(requestedUserId).isEqualTo("bob")
        assertThat(storage.get("bob")).isEqualTo(bobToken)
    }

    @Test
    fun providerFailureIsReturnedUnchanged() = runTest {
        val failure = IllegalStateException("token request failed")
        val resolver = resolver({ Result.failure(failure) }, FakeChatTokenStorage())

        assertThat(resolver.resolve("alice").exceptionOrNull()).isSameInstanceAs(failure)
    }

    @Test
    fun thrownProviderFailureIsReturnedAsFailure() = runTest {
        val failure = IllegalStateException("provider threw")
        val resolver = resolver({ throw failure }, FakeChatTokenStorage())

        assertThat(resolver.resolve("alice").exceptionOrNull()).isSameInstanceAs(failure)
    }

    @Test
    fun cancellationFailureIsRethrown() = runTest {
        val cancellation = CancellationException("token request cancelled")
        val resolver = resolver({ Result.failure(cancellation) }, FakeChatTokenStorage())

        val error = runCatching { resolver.resolve("alice") }.exceptionOrNull()

        assertThat(error).isSameInstanceAs(cancellation)
    }

    @Test
    fun tokenReturnedAfterCancellationIsNotStored() = runTest {
        val token = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val storage = FakeChatTokenStorage()
        val resolver = resolver(
            {
                currentCoroutineContext().cancel()
                Result.success(token)
            },
            storage
        )

        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            resolver.resolve("alice")
        }

        assertThat(request.isCancelled).isTrue()
        assertThat(storage.get("alice")).isNull()
    }

    @Test
    fun storageFailureIsReturnedAsFailure() = runTest {
        val failure = IllegalStateException("storage unavailable")
        val storage = object : ChatTokenStorage {
            override fun get(userId: String): String? = throw failure
            override fun save(userId: String, token: String) = Unit
            override fun clear() = Unit
        }
        val resolver = resolver({ Result.success("token-1") }, storage)

        assertThat(resolver.resolve("alice").exceptionOrNull()).isSameInstanceAs(failure)
    }

    private fun resolver(
        provider: ChatTokenProvider,
        storage: ChatTokenStorage,
        expirationLeewaySeconds: Long = 30L
    ) = ChatTokenResolver(
        provider = provider,
        storage = storage,
        expirationLeewaySeconds = expirationLeewaySeconds,
        currentTimeMillis = { NOW_EPOCH_SECONDS * 1_000L }
    )

    private fun jwt(expirationEpochSeconds: Long): String {
        val payload = Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)
            .encode("{\"exp\":$expirationEpochSeconds}".toByteArray())
        return "header.$payload.signature"
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

    private companion object {
        const val NOW_EPOCH_SECONDS = 1_800_000_000L
    }
}
