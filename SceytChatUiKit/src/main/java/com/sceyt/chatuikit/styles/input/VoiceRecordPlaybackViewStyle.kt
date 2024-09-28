package com.sceyt.chatuikit.styles.input

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.TextStyle

data class VoiceRecordPlaybackViewStyle(
        @ColorInt var backgroundColor: Int,
        @ColorInt var playerBackgroundColor: Int,
        @ColorInt var trackColor: Int,
        @ColorInt var progressColor: Int,
        var closeIcon: Drawable?,
        var playIcon: Drawable?,
        var pauseIcon: Drawable?,
        var sendVoiceIcon: Drawable?,
        var durationTextStyle: TextStyle,
        var durationFormatter: Formatter<Long>
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var playerBackgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var trackColor: Int = UNSET_COLOR

        @ColorInt
        private var progressColor: Int = UNSET_COLOR
        private var closeIcon: Drawable? = null
        private var playIcon: Drawable? = null
        private var pauseIcon: Drawable? = null
        private var sendVoiceIcon: Drawable? = null
        private var durationTextStyle: TextStyle = TextStyle()
        private var durationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter


        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun playerBackgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = playerBackgroundColor) = apply {
            this.playerBackgroundColor = typedArray.getColor(index, defValue)
        }

        fun trackColor(@StyleableRes index: Int, @ColorInt defValue: Int = trackColor) = apply {
            this.trackColor = typedArray.getColor(index, defValue)
        }

        fun progressColor(@StyleableRes index: Int, @ColorInt defValue: Int = progressColor) = apply {
            this.progressColor = typedArray.getColor(index, defValue)
        }

        fun closeIcon(@StyleableRes index: Int, defValue: Drawable? = closeIcon) = apply {
            this.closeIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun playIcon(@StyleableRes index: Int, defValue: Drawable? = playIcon) = apply {
            this.playIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun pauseIcon(@StyleableRes index: Int, defValue: Drawable? = pauseIcon) = apply {
            this.pauseIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun sendVoiceIcon(@StyleableRes index: Int, defValue: Drawable? = sendVoiceIcon) = apply {
            this.sendVoiceIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun durationTextStyle(durationTextStyle: TextStyle) = apply {
            this.durationTextStyle = durationTextStyle
        }

        fun build() = VoiceRecordPlaybackViewStyle(
            backgroundColor = backgroundColor,
            playerBackgroundColor = playerBackgroundColor,
            trackColor = trackColor,
            progressColor = progressColor,
            closeIcon = closeIcon,
            playIcon = playIcon,
            pauseIcon = pauseIcon,
            sendVoiceIcon = sendVoiceIcon,
            durationTextStyle = durationTextStyle,
            durationFormatter = durationFormatter
        )
    }
}