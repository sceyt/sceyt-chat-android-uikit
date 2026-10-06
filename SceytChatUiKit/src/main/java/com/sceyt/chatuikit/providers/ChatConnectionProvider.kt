package com.sceyt.chatuikit.providers

fun interface ChatConnectionProvider {
    /** Returns success only after the Chat SDK is connected. */
    suspend fun connect(timeoutMillis: Long): Result<Unit>
}
