package com.sceyt.chat.ui.sceytconfigs

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.chat.ui.R

object SearchInputViewStyle {
    @DrawableRes
    var searchIcon: Int = R.drawable.ic_search

    @DrawableRes
    var clearIcon: Int = R.drawable.ic_cancel

    @ColorRes
    var textColor: Int = R.color.black

    @ColorRes
    var hintTextColor: Int = R.color.hintColor

    lateinit var hintText: String

    var disableDebouncedSearch: Boolean = false


    internal fun updateWithAttributes(context: Context, typedArray: TypedArray): SearchInputViewStyle {
        searchIcon = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiSearchIcon, searchIcon)
        clearIcon = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiClearIcon, clearIcon)
        textColor = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiTextColor, textColor)
        hintTextColor = typedArray.getResourceId(R.styleable.SearchInputView_sceytUiHintTextColor, hintTextColor)
        hintText = typedArray.getString(R.styleable.SearchInputView_sceytUiHintText)
                ?: context.getString(R.string.search)
        disableDebouncedSearch = typedArray.getBoolean(R.styleable.SearchInputView_sceytUiDisableDebouncedSearch, disableDebouncedSearch)
        return this
    }
}