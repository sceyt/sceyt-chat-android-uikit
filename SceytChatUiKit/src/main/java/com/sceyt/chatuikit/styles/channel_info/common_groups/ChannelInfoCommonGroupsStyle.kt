package com.sceyt.chatuikit.styles.channel_info.common_groups

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.groups.ChannelInfoCommonGroupsFragment
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoCommonGroupsFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property loadMoreProgressColor - color for load more progress indicator
 * @property emptyState - layout resource for empty state, default is [R.layout.sceyt_empty_state]
 * @property loadingState - layout resource for loading state, default is [R.layout.sceyt_page_loading_state]
 * @property emptyStateStyle - style for empty state view with icon, title, and subtitle customization
 * @property itemStyle - style for common group item
 * */
data class ChannelInfoCommonGroupsStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val loadMoreProgressColor: Int,
        @param:LayoutRes val emptyState: Int,
        @param:LayoutRes val loadingState: Int,
        val emptyStateStyle: EmptyStateStyle,
        val itemStyle: CommonGroupItemStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoCommonGroupsStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        @ColorInt
        private var initialLoaderColor: Int = UNSET_COLOR
        @ColorInt
        private var loadMoreProgressColor: Int = UNSET_COLOR

        fun backgroundColor(@ColorInt backgroundColor: Int) = apply {
            this.backgroundColor = backgroundColor
        }

        fun build(): ChannelInfoCommonGroupsStyle {
            if (backgroundColor == UNSET_COLOR)
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            if (initialLoaderColor == UNSET_COLOR)
                initialLoaderColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)

            if (loadMoreProgressColor == UNSET_COLOR)
                loadMoreProgressColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)

            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.ChannelInfoCommonGroupsView)
            val itemStyle = try {
                CommonGroupItemStyle.Builder(context, typedArray).build()
            } finally {
                typedArray.recycle()
            }

            val emptyState = R.layout.sceyt_empty_state
            val loadingState = R.layout.sceyt_page_loading_state

            val emptyStateStyle = buildEmptyStateStyle(
                context = context,
                iconRes = R.drawable.sceyt_ic_empty_common_groups,
                titleText = context.getString(R.string.sceyt_no_common_groups_title),
                subtitleText = context.getString(R.string.sceyt_no_common_groups_desc)
            )

            return ChannelInfoCommonGroupsStyle(
                backgroundColor = backgroundColor,
                loadMoreProgressColor = loadMoreProgressColor,
                emptyState = emptyState,
                loadingState = loadingState,
                emptyStateStyle = emptyStateStyle,
                itemStyle = itemStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}