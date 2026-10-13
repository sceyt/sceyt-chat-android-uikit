package com.sceyt.chat.connection

data class ChatConnectionConfig(
    val reconnectOnForeground: Boolean = true,
    val backgroundConnectionPolicy: BackgroundConnectionPolicy = BackgroundConnectionPolicy.KeepConnected,
    val tokenExpirationLeewaySeconds: Long = 30L,
    val tokenRefreshErrorCodes: Set<Int> = setOf(TOKEN_EXPIRED_ERROR_CODE)
) {
    init {
        require(tokenExpirationLeewaySeconds >= 0L) {
            "tokenExpirationLeewaySeconds must not be negative"
        }
    }

    companion object {
        const val TOKEN_EXPIRED_ERROR_CODE = 1021
    }
}

sealed interface BackgroundConnectionPolicy {
    data object KeepConnected : BackgroundConnectionPolicy

    data class Disconnect(
        val delayMillis: Long = 0L
    ) : BackgroundConnectionPolicy {
        init {
            require(delayMillis >= 0L) { "delayMillis must not be negative" }
        }
    }
}
