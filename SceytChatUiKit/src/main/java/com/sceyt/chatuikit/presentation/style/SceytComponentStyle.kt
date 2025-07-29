package com.sceyt.chatuikit.presentation.style

import java.util.UUID

abstract class SceytComponentStyle {
    open val styleId: String = UUID.randomUUID().toString()
}