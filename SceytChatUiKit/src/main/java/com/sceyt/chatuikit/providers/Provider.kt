 package com.sceyt.chatuikit.providers

interface Provider<From, To> {
    fun provide(from: From): To
}