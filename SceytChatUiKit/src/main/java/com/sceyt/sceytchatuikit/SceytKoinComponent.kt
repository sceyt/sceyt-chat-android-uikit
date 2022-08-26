package com.sceyt.sceytchatuikit

import org.koin.core.Koin
import org.koin.core.component.KoinComponent

internal interface SceytKoinComponent : KoinComponent {
    // Override default Koin instance, initially target on GlobalContext to yours
    override fun getKoin(): Koin = MyKoinContext.koinApp?.koin
            ?: error("KoinApplication has not been started")
}