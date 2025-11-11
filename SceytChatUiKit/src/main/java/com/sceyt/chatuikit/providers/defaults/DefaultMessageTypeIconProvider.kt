package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultMessageTypeIconProvider : VisualProvider<SceytMessage, Drawable?> {
    override fun provide(context: Context, from: SceytMessage): Drawable? {
        val drawableId = when (from.type) {
            SceytMessageType.Poll.value -> R.drawable.sceyt_ic_poll_filled
            else -> return null
        }
        return context.getCompatDrawable(drawableId)?.applyTint(
            context = context,
            tintColorRes = SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    }
}