package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class GlobalSearchStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val dividerColor: Int,
    @param:ColorInt val navigationIconColor: Int,
    // Legacy properties — used by old tab adapters/fragments until they are refactored
    @param:ColorInt val highlightTextColor: Int,
    val titleTextStyle: TextStyle,
    val subtitleTextStyle: TextStyle,
    val metaTextStyle: TextStyle,
    val emptyTitleTextStyle: TextStyle,
    val emptySubtitleTextStyle: TextStyle,
    val avatarStyle: AvatarStyle,
    // Sub-styles
    val inputStyle: GlobalSearchInputStyle,
    val tabBarStyle: GlobalSearchTabBarStyle,
    val suggestionsStyle: GlobalSearchSuggestionsStyle,
    val chatsPageStyle: ChatsSearchPageStyle,
) : SceytComponentStyle() {

    internal class Builder(
        private val context: Context,
    ) {
        fun build(): GlobalSearchStyle {
            val colors = SceytChatUIKit.theme.colors
            return GlobalSearchStyle(
                backgroundColor = context.getCompatColor(colors.backgroundColor),
                dividerColor = context.getCompatColor(colors.borderColor),
                navigationIconColor = context.getCompatColor(colors.accentColor),
                highlightTextColor = context.getCompatColor(colors.accentColor),
                titleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textPrimaryColor),
                    font = R.font.roboto_medium
                ),
                subtitleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textPrimaryColor),
                    font = R.font.roboto_regular
                ),
                metaTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textSecondaryColor),
                    font = R.font.roboto_regular
                ),
                emptyTitleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textPrimaryColor),
                    font = R.font.roboto_medium
                ),
                emptySubtitleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textSecondaryColor),
                    font = R.font.roboto_regular
                ),
                avatarStyle = AvatarStyle(),
                inputStyle = GlobalSearchInputStyle.Builder(context).build(),
                tabBarStyle = GlobalSearchTabBarStyle.Builder(context).build(),
                suggestionsStyle = GlobalSearchSuggestionsStyle.Builder(context).build(),
                chatsPageStyle = ChatsSearchPageStyle.Builder(context).build()
            )
        }
    }
}
