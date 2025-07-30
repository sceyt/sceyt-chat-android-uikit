package com.sceyt.chatuikit.styles.channel_info.media

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for conversation info media
 * @property backgroundColor - background color, default is [Colors.backgroundColorSections]
 * @property emptyState - layout resource for empty state, default is [R.layout.sceyt_empty_state]
 * @property loadingState - layout resource for loading state, default is [R.layout.sceyt_page_loading_state]
 * @property emptyStateTitle - title for empty state, default is [R.string.sceyt_no_media_items_yet]
 * @property itemStyle - style for media item
 * @property dateSeparatorStyle - style for date separator
 * */
data class ChannelInfoMediaStyle(
        @ColorInt val backgroundColor: Int,
        @LayoutRes val emptyState: Int,
        @LayoutRes val loadingState: Int,
        val emptyStateTitle: String,
        val itemStyle: ChannelInfoMediaItemStyle,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoMediaStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): ChannelInfoMediaStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            val emptyState = R.layout.sceyt_empty_state
            val loadingState = R.layout.sceyt_page_loading_state
            val emptyStateTitle = context.getString(R.string.sceyt_no_media_items_yet)

            val itemStyle = ChannelInfoMediaItemStyle.Builder(context, attributeSet).build()
            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()

            return ChannelInfoMediaStyle(
                backgroundColor = backgroundColor,
                emptyState = emptyState,
                loadingState = loadingState,
                emptyStateTitle = emptyStateTitle,
                itemStyle = itemStyle,
                dateSeparatorStyle = dateSeparatorStyle,
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
