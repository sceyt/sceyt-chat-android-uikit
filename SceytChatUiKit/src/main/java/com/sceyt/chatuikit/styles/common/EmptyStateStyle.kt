package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_RESOURCE

data class EmptyStateStyle(
    val icon: Drawable? = null,
    @param:ColorInt val iconTint: Int = UNSET_COLOR,
    val titleStyle: TextStyle = TextStyle(),
    val titleText: String? = null,
    val subtitleStyle: TextStyle = TextStyle(),
    val subtitleText: String? = null,
) {

    internal class Builder(
        private val context: Context,
        private val typedArray: TypedArray? = null,
    ) {
        private var icon: Drawable? = null

        @DrawableRes
        private var iconRes: Int = UNSET_RESOURCE

        @ColorInt
        private var iconTint: Int = UNSET_COLOR

        private var titleStyle: TextStyle = TextStyle()
        private var titleText: String? = null

        private var subtitleStyle: TextStyle = TextStyle()
        private var subtitleText: String? = null

        fun setIcon(@DrawableRes drawableRes: Int) = apply {
            this.iconRes = drawableRes
        }

        fun setIconTint(@ColorInt color: Int) = apply {
            this.iconTint = color
        }

        fun setTitleStyle(style: TextStyle) = apply {
            this.titleStyle = style
        }

        fun setTitleText(text: String?) = apply {
            this.titleText = text
        }

        fun setSubtitleStyle(style: TextStyle) = apply {
            this.subtitleStyle = style
        }

        fun setSubtitleText(text: String?) = apply {
            this.subtitleText = text
        }

        fun build(): EmptyStateStyle {
            val finalIcon = icon ?: if (iconRes != UNSET_RESOURCE) {
                ContextCompat.getDrawable(context, iconRes)
            } else null

            return EmptyStateStyle(
                icon = finalIcon,
                iconTint = iconTint,
                titleStyle = titleStyle,
                titleText = titleText,
                subtitleStyle = subtitleStyle,
                subtitleText = subtitleText
            )
        }
    }
}