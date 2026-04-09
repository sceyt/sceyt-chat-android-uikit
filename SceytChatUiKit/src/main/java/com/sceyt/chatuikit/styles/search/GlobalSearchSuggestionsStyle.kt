package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSuggestionBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSuggestionTextStyle

data class GlobalSearchSuggestionsStyle(
    val suggestionBackgroundStyle: BackgroundStyle,
    val suggestionTextStyle: TextStyle,
    @param:ColorInt val suggestionIconColor: Int,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<GlobalSearchSuggestionsStyle> { _, style -> style }
    }

    internal class Builder(internal val context: Context) {
        fun build(): GlobalSearchSuggestionsStyle {
            val colors = SceytChatUIKit.theme.colors
            return GlobalSearchSuggestionsStyle(
                suggestionBackgroundStyle = buildSuggestionBackgroundStyle(),
                suggestionTextStyle = buildSuggestionTextStyle(),
                suggestionIconColor = context.getCompatColor(colors.textSecondaryColor),
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
