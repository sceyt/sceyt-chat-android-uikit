package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.renderers.VisualRenderer

data class SelectedListItemStyle<NameFormatter, AvatarRenderer>(
        val removeIcon: Drawable?,
        val textStyle: TextStyle,
        val avatarStyle: AvatarStyle,
        val presenceStateColorProvider: VisualProvider<PresenceState, Int>,
        val nameFormatter: NameFormatter,
        val avatarRenderer: AvatarRenderer,
) where NameFormatter : Formatter<*>,
        AvatarRenderer : VisualRenderer