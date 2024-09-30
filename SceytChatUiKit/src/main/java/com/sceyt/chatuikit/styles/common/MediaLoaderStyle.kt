package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class MediaLoaderStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val progressColor: Int = UNSET_COLOR,
        @ColorInt val trackColor: Int = UNSET_COLOR,
        val cancelIcon: Drawable? = null,
        val uploadIcon: Drawable? = null,
        val downloadIcon: Drawable? = null
) {

    fun apply(loader: CircularProgressView) {
        if (backgroundColor != UNSET_COLOR)
            loader.setBackgroundColor(backgroundColor)

        if (progressColor != UNSET_COLOR)
            loader.setProgressColor(progressColor)

        if (trackColor != UNSET_COLOR)
            loader.setTrackColor(trackColor)
    }

    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var trackColor: Int = UNSET_COLOR

        @ColorInt
        private var progressColor: Int = UNSET_COLOR

        private var cancelIcon: Drawable? = null
        private var uploadIcon: Drawable? = null
        private var downloadIcon: Drawable? = null

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun trackColor(@StyleableRes index: Int, @ColorInt defValue: Int = trackColor) = apply {
            this.trackColor = typedArray.getColor(index, defValue)
        }

        fun progressColor(@StyleableRes index: Int, @ColorInt defValue: Int = progressColor) = apply {
            this.progressColor = typedArray.getColor(index, defValue)
        }

        fun cancelIcon(@StyleableRes index: Int, defValue: Drawable? = cancelIcon) = apply {
            this.cancelIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun uploadIcon(@StyleableRes index: Int, defValue: Drawable? = uploadIcon) = apply {
            this.uploadIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun downloadIcon(@StyleableRes index: Int, defValue: Drawable? = downloadIcon) = apply {
            this.downloadIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun build() = MediaLoaderStyle(
            trackColor = trackColor,
            progressColor = progressColor,
            backgroundColor = backgroundColor,
            cancelIcon = cancelIcon,
            uploadIcon = uploadIcon,
            downloadIcon = downloadIcon
        )
    }
}
