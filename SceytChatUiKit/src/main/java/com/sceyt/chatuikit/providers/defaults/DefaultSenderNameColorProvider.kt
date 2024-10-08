package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.persistence.mappers.isDeleted
import com.sceyt.chatuikit.providers.VisualProvider

data object DefaultSenderNameColorProvider : VisualProvider<SceytUser, Int> {
    override fun provide(context: Context, from: SceytUser): Int {
        val colorId = if (from.isDeleted())
            SceytChatUIKit.theme.colors.errorColor else SceytChatUIKit.theme.colors.accentColor

        return context.getCompatColor(colorId)
    }
}