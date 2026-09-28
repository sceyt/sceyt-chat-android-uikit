package com.sceyt.chat.connection

import com.google.common.truth.Truth.assertThat
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
            token
        }

        val firstResolver = resolver(provider, storage)
        val secondResolver = resolver(provider, storage)

        assertThat(firstResolver.resolve("alice")).isEqualTo(token)
        assertThat(secondResolver.resolve("alice")).isEqualTo(token)
        assertThat(requestCount).isEqualTo(1)
    }

    @Test
    fun expiredStoredJwtIsReplaced() = runTest {
        val expiredToken = jwt(NOW_EPOCH_SECONDS - 1L)
        val freshToken = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val storage = FakeChatTokenStorage().apply {
            save("alice", expiredToken)
        }
        val resolver = resolver({ freshToken }, storage)

        assertThat(resolver.resolve("alice")).isEqualTo(freshToken)
        assertThat(storage.get("alice")).isEqualTo(freshToken)
    }

    @Test
    fun forceRefreshReplacesReusableJwt() = runTest {
        val firstToken = jwt(NOW_EPOCH_SECONDS + 3_600L)
        val secondToken = jwt(NOW_EPOCH_SECONDS + 7_200L)
        val tokens = listOf(firstToken, secondToken).iterator()
        val storage = FakeChatTokenStorage()
        val resolver = resolver({ tokens.next() }, storage)

        assertThat(resolver.resolve("alice")).isEqualTo(firstToken)
        assertThat(resolver.resolve("alice", forceRefresh = true)).isEqualTo(secondToken)
        assertThat(storage.get("alice")).isEqualTo(secondToken)
    }

    @Test
    fun newlyFetchedJwtInsideExpiryLeewayIsRejected() = runTest {
        val resolver = resolver(
            { jwt(NOW_EPOCH_SECONDS + 30L) },
            FakeChatTokenStorage()
        )

        val error = runCatching { resolver.resolve("alice") }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("Token is expired or expires too soon")
    }

    @Test
    fun customExpiryLeewayIsApplied() = runTest {
        val token = jwt(NOW_EPOCH_SECONDS + 30L)
        val resolver = resolver(
            provider = { token },
            storage = FakeChatTokenStorage(),
            expirationLeewaySeconds = 10L
        )

        assertThat(resolver.resolve("alice")).isEqualTo(token)
    }

    @Test
    fun opaqueTokenIsUsedButNotPersisted() = runTest {
        val storage = FakeChatTokenStorage()
        var requestCount = 0
        val resolver = resolver(
            {
                requestCount++
                "opaque-token"
            },
            storage
        )

        assertThat(resolver.resolve("alice")).isEqualTo("opaque-token")
        assertThat(resolver.resolve("alice")).isEqualTo("opaque-token")
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
                bobToken
            },
            storage
        )

        assertThat(resolver.resolve("bob")).isEqualTo(bobToken)
        assertThat(requestedUserId).isEqualTo("bob")
        assertThat(storage.get("bob")).isEqualTo(bobToken)
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
