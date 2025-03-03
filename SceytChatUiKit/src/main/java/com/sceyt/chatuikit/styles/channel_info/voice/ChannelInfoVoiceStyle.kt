package com.sceyt.chatuikit.styles.channel_info.voice

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_info.media.ChannelInfoMediaFragment
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.ChannelInfoDateSeparatorStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelInfoMediaFragment] page.
 * @property backgroundColor - background color, default is [Colors.backgroundColor]
 * @property emptyState - layout resource for empty state, default is [R.layout.sceyt_empty_state]
 * @property loadingState - layout resource for loading state, default is [R.layout.sceyt_page_loading_state]
 * @property emptyStateTitle - title for empty state, default is [R.string.sceyt_no_voice_items_yet]
 * @property dateSeparatorStyle - style for date separator
 * @property itemStyle - style for voice item
 * */
data class ChannelInfoVoiceStyle(
        @ColorInt val backgroundColor: Int,
        @LayoutRes val emptyState: Int,
        @LayoutRes val loadingState: Int,
        val emptyStateTitle: String,
        val dateSeparatorStyle: ChannelInfoDateSeparatorStyle,
        val itemStyle: ChannelInfoVoiceItemStyle,
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelInfoVoiceStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoVoiceStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)

            val emptyState = R.layout.sceyt_empty_state
            val loadingState = R.layout.sceyt_page_loading_state
            val emptyStateTitle = context.getString(R.string.sceyt_no_voice_items_yet)

            val dateSeparatorStyle = ChannelInfoDateSeparatorStyle.Builder(context, attributeSet)
                .build()

            val itemStyle = ChannelInfoVoiceItemStyle.Builder(context, attributeSet)
                .build()

            return ChannelInfoVoiceStyle(
                backgroundColor = backgroundColor,
                emptyState = emptyState,
                loadingState = loadingState,
                emptyStateTitle = emptyStateTitle,
                dateSeparatorStyle = dateSeparatorStyle,
                itemStyle = itemStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
