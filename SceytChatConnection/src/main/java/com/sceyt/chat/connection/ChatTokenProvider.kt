package com.sceyt.chat.connection

fun interface ChatTokenProvider {
    suspend fun provideToken(userId: String): String?
}
