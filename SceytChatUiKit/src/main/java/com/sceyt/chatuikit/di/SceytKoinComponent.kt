package com.sceyt.chatuikit.di

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

internal interface SceytKoinComponent : KoinComponent {
    // Override default Koin instance, initially target on GlobalContext to yours
    override fun getKoin(): Koin = SceytKoinApp.koinApp?.koin
            ?: error("KoinApplication has not been started")
}