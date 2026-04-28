package com.sceyt.chatuikit.styles.extensions.search

import androidx.core.graphics.ColorUtils
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatColorNight
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.search.GlobalSearchInputStyle

internal fun GlobalSearchInputStyle.Builder.buildSearchInputStyle(): SearchInputStyle {
    val colors = SceytChatUIKit.theme.colors
    return SearchInputStyle(
        searchIcon = context.getCompatDrawable(R.drawable.sceyt_ic_search)?.applyTint(
            context.getCompatColor(colors.iconSecondaryColor)
        ),
        clearIcon = context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.applyTint(
            context.getCompatColor(colors.iconSecondaryColor)
        ),
        textInputStyle = TextInputStyle(
            backgroundStyle = BackgroundStyle(
                backgroundColor = context.getCompatColor(colors.surface1Color),
                shape = Shape.RoundedCornerShape(10f.dpToPx())
            ),
            textStyle = TextStyle(
                color = context.getCompatColor(colors.textPrimaryColor),
                font = R.font.roboto_regular
            ),
            hintStyle = HintStyle(
                color = context.getCompatColor(colors.textFootnoteColor)
            )
        )
    )
}

internal fun GlobalSearchInputStyle.Builder.buildChipTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textSecondaryColor),
        font = R.font.roboto_regular
    )
}

internal fun GlobalSearchInputStyle.Builder.buildSelectedUserChipTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.onPrimaryColor),
        font = R.font.roboto_regular
    )
}

internal fun GlobalSearchInputStyle.Builder.buildSelectedUserChipPendingTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.onPrimaryColor),
        font = R.font.roboto_regular
    )
}

internal fun GlobalSearchInputStyle.Builder.buildChipBackgroundStyle(): BackgroundStyle {
    val colors = SceytChatUIKit.theme.colors
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(colors.backgroundColor),
        borderColor = context.getCompatColor(colors.borderColor),
        borderWidth = 1.dpToPx(),
        shape = Shape.RoundedCornerShape(30f.dpToPx())
    )
}

internal fun GlobalSearchInputStyle.Builder.buildSelectedUserChipBackgroundStyle(): BackgroundStyle {
    val colors = SceytChatUIKit.theme.colors
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(colors.surface3Color),
        shape = Shape.RoundedCornerShape(30f.dpToPx())
    )
}

internal fun GlobalSearchInputStyle.Builder.buildSelectedUserChipPendingBackgroundStyle(): BackgroundStyle {
    val accentColor = context.getCompatColorNight(SceytChatUIKit.theme.colors.accentColor)
    val bgColor = context.getCompatColorNight(SceytChatUIKit.theme.colors.backgroundColor)
    val blendedColor = ColorUtils.blendARGB(accentColor, bgColor, 0.5f)
    return BackgroundStyle(
        backgroundColor = blendedColor,
        shape = Shape.RoundedCornerShape(30f.dpToPx())
    )
}
