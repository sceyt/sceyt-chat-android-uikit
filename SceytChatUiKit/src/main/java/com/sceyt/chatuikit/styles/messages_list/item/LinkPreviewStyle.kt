package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class LinkPreviewStyle(
        val titleStyle: TextStyle,
        val descriptionStyle: TextStyle,
        val placeHolder: Drawable?
) {
    companion object {
        var styleCustomizer = StyleCustomizer<LinkPreviewStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        private var titleStyle: TextStyle = TextStyle()
        private var descriptionStyle: TextStyle = TextStyle()
        private var placeHolder: Drawable? = null


        fun titleStyle(titleStyle: TextStyle) = apply {
            this.titleStyle = titleStyle
        }

        fun descriptionStyle(descriptionStyle: TextStyle) = apply {
            this.descriptionStyle = descriptionStyle
        }

        fun placeHolder(@StyleableRes index: Int, defValue: Drawable? = placeHolder) = apply {
            this.placeHolder = typedArray.getDrawable(index) ?: defValue
        }

        fun build() = LinkPreviewStyle(
            titleStyle = titleStyle,
            descriptionStyle = descriptionStyle,
            placeHolder = placeHolder
        ).also { styleCustomizer.apply(context, it) }
    }
}