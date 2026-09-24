package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultPinnedMessageIconProvider : VisualProvider<SceytMessage, Drawable?> {
    override fun provide(context: Context, from: SceytMessage): Drawable? = null
}