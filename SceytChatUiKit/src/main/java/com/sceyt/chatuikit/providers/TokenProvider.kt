package com.sceyt.chatuikit.providers

fun interface TokenProvider {
    suspend fun provideToken(): String?
}