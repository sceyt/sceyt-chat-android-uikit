package com.sceyt.chatuikit.styles.extensions.search

import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.search.GlobalSearchTabBarStyle

internal fun GlobalSearchTabBarStyle.Builder.buildSelectedTabBackgroundStyle(): BackgroundStyle {
    val colors = SceytChatUIKit.theme.colors
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(colors.surface1Color),
        shape = Shape.RoundedCornerShape(30f.dpToPx())
    )
}

internal fun GlobalSearchTabBarStyle.Builder.buildSelectedTabTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
}

internal fun GlobalSearchTabBarStyle.Builder.buildUnselectedTabBackgroundStyle(): BackgroundStyle {
    val colors = SceytChatUIKit.theme.colors
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(colors.backgroundColor),
        borderColor = context.getCompatColor(colors.borderColor),
        borderWidth = 1.dpToPx(),
        shape = Shape.RoundedCornerShape(30f.dpToPx())
    )
}

internal fun GlobalSearchTabBarStyle.Builder.buildUnselectedTabTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textSecondaryColor),
        font = R.font.roboto_regular
    )
}
