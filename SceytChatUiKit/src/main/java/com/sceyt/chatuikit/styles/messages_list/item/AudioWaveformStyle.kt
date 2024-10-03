package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer

data class AudioWaveformStyle(
        @ColorInt val trackColor: Int,
        @ColorInt val progressColor: Int,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<AudioWaveformStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var trackColor: Int = UNSET_COLOR

        @ColorInt
        private var progressColor: Int = UNSET_COLOR

        fun trackColor(@StyleableRes index: Int, @ColorInt defValue: Int = trackColor) = apply {
            this.trackColor = typedArray.getColor(index, defValue)
        }

        fun progressColor(@StyleableRes index: Int, @ColorInt defValue: Int = progressColor) = apply {
            this.progressColor = typedArray.getColor(index, defValue)
        }

        fun build() = AudioWaveformStyle(
            trackColor = trackColor,
            progressColor = progressColor
        ).let { styleCustomizer.apply(context, it) }
    }
}