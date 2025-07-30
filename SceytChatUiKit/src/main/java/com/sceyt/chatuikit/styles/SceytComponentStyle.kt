package com.sceyt.chatuikit.styles

import java.util.UUID

abstract class SceytComponentStyle {
    open val styleId: String = UUID.randomUUID().toString()
}