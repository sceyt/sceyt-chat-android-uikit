package com.sceyt.chatuikit.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.extensions.buildSearchInputStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for the search channel input view.
 * @property backgroundColor Background color for the input, default is [SceytChatUIKitTheme.surface1Color]
 * @property searchInputStyle Style for the search input, default is [buildSearchInputStyle].
 * */
data class SearchChannelInputStyle(
        @ColorInt var backgroundColor: Int,
        var searchInputStyle: SearchInputStyle
) {

    companion object {
        var styleCustomizer = StyleCustomizer<SearchChannelInputStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): SearchChannelInputStyle {
            context.obtainStyledAttributes(attrs, R.styleable.SearchChannelInputView).use { array ->
                val backgroundColor = array.getColor(R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.surface1Color))

                return SearchChannelInputStyle(
                    backgroundColor = backgroundColor,
                    searchInputStyle = buildSearchInputStyle(array),
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}