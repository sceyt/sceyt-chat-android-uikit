package com.sceyt.chat.connection

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class ChatTokenResolver(
    private val provider: ChatTokenProvider,
    private val storage: ChatTokenStorage,
    private val expirationLeewaySeconds: Long,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun resolve(userId: String, forceRefresh: Boolean = false): String? {
        if (forceRefresh) {
            invalidate()
        } else {
            reusableToken(userId)?.let { return it }
        }

        val token = provider.provideToken(userId)
        currentCoroutineContext().ensureActive()
        if (token.isNullOrBlank()) return token

        val expirationEpochSeconds = token.jwtExpirationEpochSeconds()
        if (expirationEpochSeconds != null && !isReusable(expirationEpochSeconds)) {
            throw IllegalStateException("Token is expired or expires too soon")
        }

        if (expirationEpochSeconds != null) {
            storage.save(userId, token)
        } else {
            storage.clear()
        }
        return token
    }

    fun invalidate() {
        storage.clear()
    }

    private fun reusableToken(userId: String): String? {
        val token = storage.get(userId) ?: return null
        val expirationEpochSeconds = token.jwtExpirationEpochSeconds()
        if (expirationEpochSeconds == null || !isReusable(expirationEpochSeconds)) {
            invalidate()
            return null
        }
        return token
    }

    private fun isReusable(expirationEpochSeconds: Long): Boolean =
        expirationEpochSeconds > currentTimeMillis() / MILLIS_PER_SECOND + expirationLeewaySeconds

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
