package com.sceyt.chatuikit.styles.channel_info.link

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoLinksFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property emptyState - layout resource for empty state, default is [R.layout.sceyt_empty_state]
 * @property loadingState - layout resource for loading state, default is [R.layout.sceyt_page_loading_state]
 * @property emptyStateStyle - style for empty state view with icon, title, and subtitle customization
 * @property itemStyle - style for link item
 * @property dateSeparatorStyle - style for date separator
 * */
data class ChannelInfoLinkStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:LayoutRes val emptyState: Int,
        @param:LayoutRes val loadingState: Int,
        val emptyStateStyle: EmptyStateStyle,
        val itemStyle: ChannelInfoLinkItemStyle,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoLinkStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): ChannelInfoLinkStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            val emptyState = R.layout.sceyt_empty_state
            val loadingState = R.layout.sceyt_page_loading_state

            val emptyStateStyle = buildEmptyStateStyle(
                context = context,
                iconRes = R.drawable.sceyt_ic_empty_links,
                titleText = context.getString(R.string.sceyt_no_links_title),
                subtitleText = context.getString(R.string.sceyt_no_links_desc)
            )

            val itemStyle = ChannelInfoLinkItemStyle.Builder(context, attributeSet).build()
            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()


            return ChannelInfoLinkStyle(
                backgroundColor = backgroundColor,
                emptyState = emptyState,
                loadingState = loadingState,
                emptyStateStyle = emptyStateStyle,
                itemStyle = itemStyle,
                dateSeparatorStyle = dateSeparatorStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}