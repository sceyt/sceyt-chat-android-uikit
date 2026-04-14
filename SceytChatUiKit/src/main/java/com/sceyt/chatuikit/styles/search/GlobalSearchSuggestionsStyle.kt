package com.sceyt.chatuikit.styles.search

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSuggestionAvatarStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSuggestionBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSuggestionTextStyle

data class GlobalSearchSuggestionsStyle(
    val suggestionBackgroundStyle: BackgroundStyle,
    val suggestionTextStyle: TextStyle,
    val avatarStyle: AvatarStyle,
    val userNameFormatter: Formatter<SceytUser>,
    val avatarRenderer: AvatarRenderer<SceytUser>
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<GlobalSearchSuggestionsStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        internal val attrs: AttributeSet? = null
    ) {
        fun build(): GlobalSearchSuggestionsStyle =
            context.obtainStyledAttributes(attrs, R.styleable.GlobalSearchSuggestionsView).use {
                return GlobalSearchSuggestionsStyle(
                    suggestionBackgroundStyle = buildSuggestionBackgroundStyle(),
                    suggestionTextStyle = buildSuggestionTextStyle(),
                    avatarStyle = buildSuggestionAvatarStyle(it),
                    userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                    avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer,
                ).let { style -> styleCustomizer.apply(context, style) }
            }
    }
}
