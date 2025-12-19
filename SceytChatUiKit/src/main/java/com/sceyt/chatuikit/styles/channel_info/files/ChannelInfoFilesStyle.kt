package com.sceyt.chatuikit.styles.channel_info.files

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.files.ChannelInfoFilesFragment
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoFilesFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property emptyState - layout resource for empty state, default is [R.layout.sceyt_empty_state]
 * @property loadingState - layout resource for loading state, default is [R.layout.sceyt_page_loading_state]
 * @property emptyStateStyle - style for empty state view with icon, title, and subtitle customization
 * @property dateSeparatorStyle - style for date separator
 * @property itemStyle - style for file item
 * */
data class ChannelInfoFilesStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:LayoutRes val emptyState: Int,
        @param:LayoutRes val loadingState: Int,
        val emptyStateStyle: EmptyStateStyle,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
        val itemStyle: ChannelInfoFileItemStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoFilesStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): ChannelInfoFilesStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            val itemStyle = ChannelInfoFileItemStyle.Builder(context, attributeSet).build()

            val emptyState = R.layout.sceyt_empty_state
            val loadingState = R.layout.sceyt_page_loading_state

            val emptyStateStyle = buildEmptyStateStyle(
                context = context,
                iconRes = R.drawable.sceyt_ic_empty_files,
                titleText = context.getString(R.string.sceyt_no_files_title),
                subtitleText = context.getString(R.string.sceyt_no_files_desc)
            )

            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()

            return ChannelInfoFilesStyle(
                backgroundColor = backgroundColor,
                emptyState = emptyState,
                loadingState = loadingState,
                emptyStateStyle = emptyStateStyle,
                dateSeparatorStyle = dateSeparatorStyle,
                itemStyle = itemStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
