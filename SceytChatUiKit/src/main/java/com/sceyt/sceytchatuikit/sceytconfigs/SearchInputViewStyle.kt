package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R

object SearchInputViewStyle {
    @DrawableRes
    var searchIcon: Int = R.drawable.sceyt_ic_search

    @DrawableRes
    var clearIcon: Int = R.drawable.sceyt_ic_cancel

    @ColorRes
    var textColor: Int = R.color.sceyt_color_black_themed

    @ColorRes
    var hintTextColor: Int = R.color.sceyt_color_hint

    @ColorRes
    var backgroundColor: Int = R.color.sceyt_color_input

    lateinit var hintText: String

    var disableDebouncedSearch: Boolean = false


    internal fun updateWithAttributes(context: Context, typedArray: TypedArray): SearchInputViewStyle {
        searchIcon = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiSearchIcon, searchIcon)
        clearIcon = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiClearIcon, clearIcon)
        textColor = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiTextColor, textColor)
        hintTextColor = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiHintTextColor, hintTextColor)
        backgroundColor = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiBackgroundColor, backgroundColor)
        hintText = typedArray.getString(R.styleable.SearchInputView_sceytUiHintText)
                ?: context.getString(R.string.sceyt_search_for_channels)
        disableDebouncedSearch = typedArray.getBoolean(R.styleable.SearchInputView_sceytUiDisableDebouncedSearch, disableDebouncedSearch)
        return this
    }
}