package com.sceyt.chat.connection

fun interface ChatTokenProvider {
    /** Returns a token or the fetch failure. Coroutine cancellation must propagate. */
    suspend fun provideToken(userId: String): Result<String>
}
