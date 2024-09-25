package com.sceyt.chatuikit.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [ChannelListView] component.
 * @property backgroundColor - Background color of the channel list, default is [SceytChatUIKitTheme.backgroundColor].
 * @property emptyState - Layout for empty state, default is [R.layout.sceyt_channel_list_empty_state].
 * @property emptySearchState - Layout for empty search state, default is [R.layout.sceyt_search_channels_empty_state].
 * @property loadingState - Layout for loading state, default is [R.layout.sceyt_channels_page_loading_state].
 * @property popupStyle - Style for popup, default is [R.style.SceytPopupMenuStyle].
 * @property showChannelActionAsPopup - Show channel action as popup, default is false.
 * @property itemStyle - Style for channel item.
 * */
data class ChannelListViewStyle(
        @ColorInt val backgroundColor: Int,
        @LayoutRes val emptyState: Int,
        @LayoutRes val emptySearchState: Int,
        @LayoutRes val loadingState: Int,
        @StyleRes val popupStyle: Int,
        val showChannelActionAsPopup: Boolean,
        val itemStyle: ChannelItemStyle
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<ChannelListViewStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {

        fun build(): ChannelListViewStyle {
            context.obtainStyledAttributes(attrs, R.styleable.ChannelListView).use { array ->
                val backgroundColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

                val emptyState = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListEmptyStateView,
                    R.layout.sceyt_channel_list_empty_state)

                val emptySearchState = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListEmptySearchStateView,
                    R.layout.sceyt_search_channels_empty_state)

                val loadingState = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListLoadingView,
                    R.layout.sceyt_channels_page_loading_state)

                val showChannelActionAsPopup = array.getBoolean(R.styleable.ChannelListView_sceytUiChannelListShowChannelActionAsPopup, false)

                val popupStyle = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListPopupStyle,
                    R.style.SceytPopupMenuStyle)

                val itemStyle = ChannelItemStyle.Builder(context, attrs).build()

                return ChannelListViewStyle(
                    backgroundColor = backgroundColor,
                    emptyState = emptyState,
                    emptySearchState = emptySearchState,
                    loadingState = loadingState,
                    popupStyle = popupStyle,
                    showChannelActionAsPopup = showChannelActionAsPopup,
                    itemStyle = itemStyle,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}