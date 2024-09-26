package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.StyleableRes

data class SearchInputStyle(
        val searchIcon: Drawable? = null,
        val clearIcon: Drawable? = null,
        val textInputStyle: TextInputStyle = TextInputStyle(),
) {

    /**
     * Applies the style to the provided views.
     * @param editText EditText view to apply the style.
     * @param inputRoot If not null, the background will be applied to editText, otherwise to inputRoot.
     * @param searchIconImage ImageView to apply the search icon.
     * @param clearIconImage ImageView to apply the clear icon.
     * */
    fun apply(
            editText: EditText,
            inputRoot: View?,
            searchIconImage: ImageView?,
            clearIconImage: ImageView?
    ) {
        textInputStyle.apply(editText, inputRoot)
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