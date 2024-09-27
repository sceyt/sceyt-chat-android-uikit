package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import androidx.annotation.ColorInt
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.PresenceState.*
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.providers.VisualProvider

data object DefaultPresenceStateColorProvider : VisualProvider<PresenceState, Int> {

    @ColorInt
    override fun provide(context: Context, from: PresenceState): Int {
        return when (from) {
            Online -> context.getCompatColor(R.color.sceyt_color_green)
            else -> 0
        }
    }
}