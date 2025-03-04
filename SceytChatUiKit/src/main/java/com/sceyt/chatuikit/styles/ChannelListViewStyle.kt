package com.sceyt.chatuikit.styles

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_list.channels.ChannelListView
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [ChannelListView] component.
 * @property backgroundColor - Background color of the channel list, default is [Colors.backgroundColor].
 * @property emptyState - Layout for empty state, default is [R.layout.sceyt_channel_list_empty_state].
 * @property emptySearchState - Layout for empty search state, default is [R.layout.sceyt_search_channels_empty_state].
 * @property loadingState - Layout for loading state, default is [R.layout.sceyt_page_loading_state].
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

        /**
         * Use this method if you are using [ChannelListView] in multiple places,
         * and want to customize the style for each view.
         * @param viewId - Id of the current [ChannelListView] which you want to customize.
         * @param customizer - Customizer for [ChannelListViewStyle].
         *
         * Note: If you have already set the [styleCustomizer], it will be overridden by this customizer.
         * */
        @Suppress("unused")
        @JvmStatic
        fun setStyleCustomizerForViewId(viewId: Int, customizer: StyleCustomizer<ChannelListViewStyle>) {
            styleCustomizers[viewId] = customizer
        }

        private var styleCustomizers: HashMap<Int, StyleCustomizer<ChannelListViewStyle>> = hashMapOf()
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {

        fun build(): ChannelListViewStyle {
            context.obtainStyledAttributes(attrs, R.styleable.ChannelListView).use { array ->
                val viewId = array.getResourceId(R.styleable.ChannelListView_android_id, View.NO_ID)

                val backgroundColor = array.getColor(R.styleable.ChannelListView_sceytUiChannelListBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))

                val emptyState = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListEmptyStateView,
                    R.layout.sceyt_channel_list_empty_state)

                val emptySearchState = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListEmptySearchStateView,
                    R.layout.sceyt_search_channels_empty_state)

                val loadingState = array.getResourceId(R.styleable.ChannelListView_sceytUiChannelListLoadingView,
                    R.layout.sceyt_page_loading_state)

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
                ).let {
                    (styleCustomizers[viewId] ?: styleCustomizer).apply(context, it)
                }
            }
        }
    }
}