package com.sceyt.chatuikit.styles.search

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle

data class GlobalSearchStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val dividerColor: Int,
    @param:ColorInt val navigationIconColor: Int,
    // Sub-styles
    val inputStyle: GlobalSearchInputStyle,
    val tabBarStyle: GlobalSearchTabBarStyle,
    val suggestionsStyle: GlobalSearchSuggestionsStyle,
    val chatsPageStyle: ChatsSearchPageStyle,
    val channelsPageStyle: ChannelsSearchPageStyle,
    val mediaPageStyle: MediaSearchPageStyle,
) : SceytComponentStyle() {

    internal class Builder(
        private val context: Context,
        private val attrs: AttributeSet? = null,
    ) {
        fun build(): GlobalSearchStyle {
            val colors = SceytChatUIKit.theme.colors
            return GlobalSearchStyle(
                backgroundColor = context.getCompatColor(colors.backgroundColor),
                dividerColor = context.getCompatColor(colors.borderColor),
                navigationIconColor = context.getCompatColor(colors.accentColor),
                inputStyle = GlobalSearchInputStyle.Builder(context).build(),
                tabBarStyle = GlobalSearchTabBarStyle.Builder(context).build(),
                suggestionsStyle = GlobalSearchSuggestionsStyle.Builder(context).build(),
                chatsPageStyle = ChatsSearchPageStyle.Builder(context).build(),
                channelsPageStyle = ChannelsSearchPageStyle.Builder(context).build(),
                mediaPageStyle = MediaSearchPageStyle.Builder(context, attrs).build(),
            )
        }
    }
}
