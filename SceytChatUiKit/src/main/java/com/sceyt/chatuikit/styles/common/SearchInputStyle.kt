package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.StyleableRes

data class SearchInputStyle(
        val searchIcon: Drawable? = null,
        val clearIcon: Drawable? = null,
        val textInputStyle: TextInputStyle = TextInputStyle(),
) {
    fun apply(
            editText: EditText,
            searchIconImage: ImageView?,
            clearIconImage: ImageView?
    ) {
        textInputStyle.apply(editText)
        searchIconImage?.setImageDrawable(searchIcon)
        clearIconImage?.setImageDrawable(clearIcon)
    }

    internal class Builder(private val typedArray: TypedArray) {
        private var searchIcon: Drawable? = null
        private var clearIcon: Drawable? = null
        private var textInputStyle: TextInputStyle = TextInputStyle()

        fun searchIcon(@StyleableRes index: Int, defValue: Drawable? = searchIcon) = apply {
            searchIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun clearIcon(@StyleableRes index: Int, defValue: Drawable? = clearIcon) = apply {
            clearIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun textInputStyle(textInputStyle: TextInputStyle) = apply {
            this.textInputStyle = textInputStyle
        }

        fun build() = SearchInputStyle(
            searchIcon = searchIcon,
            clearIcon = clearIcon,
            textInputStyle = textInputStyle
        )
    }
}