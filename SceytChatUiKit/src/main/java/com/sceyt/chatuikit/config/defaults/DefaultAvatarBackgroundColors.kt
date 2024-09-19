package com.sceyt.chatuikit.config.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor

open class DefaultAvatarBackgroundColors(context: Context) {
    open val colors = listOf(
        context.getCompatColor(SceytChatUIKit.theme.accentColor),
        context.getCompatColor(SceytChatUIKit.theme.accentColor2),
        context.getCompatColor(SceytChatUIKit.theme.accentColor3),
        context.getCompatColor(SceytChatUIKit.theme.accentColor4),
        context.getCompatColor(SceytChatUIKit.theme.accentColor5),
    )
}
