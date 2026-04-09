package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle

data class ChatsSearchPageStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:LayoutRes val emptyState: Int,
    @param:LayoutRes val emptySearchState: Int,
    @param:LayoutRes val loadingState: Int,
    val emptyStateStyle: EmptyStateStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChatsSearchPageStyle> { _, style -> style }
    }

    internal class Builder(private val context: Context) {
        fun build() = ChatsSearchPageStyle(
            backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
            emptyState = R.layout.sceyt_channel_list_empty_state,
            emptySearchState = R.layout.sceyt_channel_list_empty_state,
            loadingState = R.layout.sceyt_page_loading_state,
            emptyStateStyle = buildEmptyStateStyle(
                context = context,
                iconRes = R.drawable.sceyt_ic_search,
                titleText = context.getString(R.string.sceyt_ui_channel_list_empty),
                subtitleText = context.getString(R.string.sceyt_ui_channel_list_empty_desc)
            ),
        ).let { styleCustomizer.apply(context, it) }
    }
}
