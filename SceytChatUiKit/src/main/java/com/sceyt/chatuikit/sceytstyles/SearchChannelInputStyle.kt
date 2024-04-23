package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for the search channel input view.
 * @param searchIcon Icon for the search button, default is [R.drawable.sceyt_ic_search]
 * @param clearIcon Icon for the clear button, default is [R.drawable.sceyt_ic_cancel]
 * @param textColor Text color for the input, default is [SceytChatUIKitTheme.textPrimaryColor]
 * @param hintTextColor Hint text color for the input, default is [SceytChatUIKitTheme.textFootnoteColor]
 * @param backgroundColor Background color for the input, default is [SceytChatUIKitTheme.surface1Color]
 * @param hintText Hint text for the input, default is "Search for channels"
 * @param disableDebouncedSearch Disable debounced search, default is false
 * */
data class SearchChannelInputStyle(
        var searchIcon: Drawable?,
        var clearIcon: Drawable?,
        @ColorInt var textColor: Int,
        @ColorInt var hintTextColor: Int,
        @ColorInt var backgroundColor: Int,
        var hintText: String,
        var disableDebouncedSearch: Boolean
) {

    companion object {
        var searchChannelInputStyleCustomizer = StyleCustomizer<SearchChannelInputStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): SearchChannelInputStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SearchChannelInput, 0, 0)

            val searchIcon = typedArray.getDrawable(R.styleable.SearchChannelInput_sceytUiSearchIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_search)?.apply {
                        setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val clearIcon = typedArray.getDrawable(R.styleable.SearchChannelInput_sceytUiClearIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.apply {
                        setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                    }

            val textColor = typedArray.getColor(R.styleable.SearchChannelInput_sceytUiTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))

            val hintTextColor = typedArray.getColor(R.styleable.SearchChannelInput_sceytUiHintTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textFootnoteColor))

            val backgroundColor = typedArray.getColor(R.styleable.SearchChannelInput_sceytUiBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.surface1Color))

            val disableDebouncedSearch = typedArray.getBoolean(R.styleable.SearchChannelInput_sceytUiDisableDebouncedSearch,
                false)

            return SearchChannelInputStyle(
                searchIcon = searchIcon,
                clearIcon = clearIcon,
                textColor = textColor,
                hintTextColor = hintTextColor,
                backgroundColor = backgroundColor,
                hintText = context.getString(R.string.sceyt_search_for_channels),
                disableDebouncedSearch = disableDebouncedSearch
            ).let(searchChannelInputStyleCustomizer::apply)
        }
    }
}