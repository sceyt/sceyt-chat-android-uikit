package com.sceyt.chatuikit.styles.extensions.search

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.search.GlobalSearchSuggestionsStyle

internal fun GlobalSearchSuggestionsStyle.Builder.buildSuggestionBackgroundStyle(): BackgroundStyle {
    val colors = SceytChatUIKit.theme.colors
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(colors.surface1Color),
        shape = Shape.RoundedCornerShape(30f.dpToPx())
    )
}

internal fun GlobalSearchSuggestionsStyle.Builder.buildSuggestionTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textPrimaryColor),
        font = R.font.roboto_regular
    )
}

internal fun GlobalSearchSuggestionsStyle.Builder.buildSuggestionAvatarStyle(
    typedArray: TypedArray
): AvatarStyle {
    return AvatarStyle.Builder(typedArray = typedArray).build()
}