package com.sceyt.chatuikit.styles.search

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel_list.search.SearchChannelInputView
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.extensions.search_channel.buildSearchInputStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for [SearchChannelInputView].
 * @property backgroundColor Background color for the input, default is [Colors.surface1Color]
 * @property searchInputStyle Style for the search input, default is [buildSearchInputStyle].
 * */
data class SearchChannelInputStyle(
        @ColorInt val backgroundColor: Int,
        val searchInputStyle: SearchInputStyle
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<SearchChannelInputStyle> { _, style -> style }

        /**
         * Use this method if you are using [SearchChannelInputView] in multiple places,
         * and want to customize the style for each view.
         * @param viewId - Id of the current [SearchChannelInputView] which you want to customize.
         * @param customizer - Customizer for [SearchChannelInputStyle].
         *
         * Note: If you have already set the [styleCustomizer], it will be overridden by this customizer.
         * */
        @Suppress("unused")
        @JvmStatic
        fun setStyleCustomizerForViewId(viewId: Int, customizer: StyleCustomizer<SearchChannelInputStyle>) {
            styleCustomizers[viewId] = customizer
        }

        private var styleCustomizers: HashMap<Int, StyleCustomizer<SearchChannelInputStyle>> = hashMapOf()
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): SearchChannelInputStyle {
            context.obtainStyledAttributes(attrs, R.styleable.SearchChannelInputView).use { array ->
                val viewId = array.getResourceId(R.styleable.SearchChannelInputView_android_id, View.NO_ID)

                val backgroundColor = array.getColor(R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color))

                return SearchChannelInputStyle(
                    backgroundColor = backgroundColor,
                    searchInputStyle = buildSearchInputStyle(array),
                ).let {
                    (styleCustomizers[viewId] ?: styleCustomizer).apply(context, it)
                }
            }
        }
    }
}