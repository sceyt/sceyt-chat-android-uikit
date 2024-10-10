package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider

data class SelectedListItemStyle<NameFormatter, AvatarProvider>(
        val removeIcon: Drawable?,
        val textStyle: TextStyle,
        val nameFormatter: NameFormatter,
        val avatarProvider: AvatarProvider,
) where NameFormatter : Formatter<*>,
        AvatarProvider : VisualProvider<*, *>