package com.sceyt.chatuikit.theme

import com.sceyt.chatuikit.SceytChatUIKit

data class SceytChatUIKitTheme(
        var colors: Colors = Colors(),
) {
    companion object {
        val colors get() = SceytChatUIKit.theme.colors
    }
}