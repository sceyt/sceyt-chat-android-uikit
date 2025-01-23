package com.sceyt.chatuikit.providers

fun interface ChatTokenProvider {
    suspend fun provideToken(): String?
}