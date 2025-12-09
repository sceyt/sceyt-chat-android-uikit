package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class VoiceRecorderViewStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val recordingIndicatorColor: Int,
        val slideToCancelText: String,
        val cancelText: String,
        val deleteRecordIcon: Drawable?,
        val lockRecordingIcon: Drawable?,
        val arrowToLockIcon: Drawable?,
        val stopRecordingIcon: Drawable?,
        val sendVoiceIcon: Drawable?,
        val slideToCancelTextStyle: TextStyle,
        val durationTextStyle: TextStyle,
        val cancelTextStyle: TextStyle,
        val durationFormatter: Formatter<Long>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<VoiceRecorderViewStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var recordingIndicatorColor: Int = UNSET_COLOR
        private var slideToCancelText: String = ""
        private var cancelText: String = ""
        private var deleteRecordIcon: Drawable? = null
        private var lockRecordingIcon: Drawable? = null
        private var arrowToLockIcon: Drawable? = null
        private var stopRecordingIcon: Drawable? = null
        private var sendVoiceIcon: Drawable? = null
        private var slideToCancelTextStyle: TextStyle = TextStyle()
        private var durationTextStyle: TextStyle = TextStyle()
        private var cancelTextStyle: TextStyle = TextStyle()
        private var durationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun recordingIndicatorColor(@StyleableRes index: Int, @ColorInt defValue: Int = recordingIndicatorColor) = apply {
            this.recordingIndicatorColor = typedArray.getColor(index, defValue)
        }

        fun slideToCancelText(@StyleableRes index: Int, defValue: String = slideToCancelText) = apply {
            this.slideToCancelText = typedArray.getString(index) ?: defValue
        }

        fun cancelText(@StyleableRes index: Int, defValue: String = cancelText) = apply {
            this.cancelText = typedArray.getString(index) ?: defValue
        }

        fun deleteRecordIcon(@StyleableRes index: Int, defValue: Drawable? = deleteRecordIcon) = apply {
            this.deleteRecordIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun lockRecordingIcon(@StyleableRes index: Int, defValue: Drawable? = lockRecordingIcon) = apply {
            this.lockRecordingIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun arrowToLockIcon(@StyleableRes index: Int, defValue: Drawable? = arrowToLockIcon) = apply {
            this.arrowToLockIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun stopRecordingIcon(@StyleableRes index: Int, defValue: Drawable? = stopRecordingIcon) = apply {
            this.stopRecordingIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun sendVoiceIcon(@StyleableRes index: Int, defValue: Drawable? = sendVoiceIcon) = apply {
            this.sendVoiceIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun slideToCancelTextStyle(slideToCancelTextStyle: TextStyle) = apply {
            this.slideToCancelTextStyle = slideToCancelTextStyle
        }

        fun durationTextStyle(durationTextStyle: TextStyle) = apply {
            this.durationTextStyle = durationTextStyle
        }

        fun cancelTextStyle(cancelTextStyle: TextStyle) = apply {
            this.cancelTextStyle = cancelTextStyle
        }

        fun build() = VoiceRecorderViewStyle(
            backgroundColor = backgroundColor,
            recordingIndicatorColor = recordingIndicatorColor,
            slideToCancelText = slideToCancelText,
            cancelText = cancelText,
            deleteRecordIcon = deleteRecordIcon,
            lockRecordingIcon = lockRecordingIcon,
            arrowToLockIcon = arrowToLockIcon,
            stopRecordingIcon = stopRecordingIcon,
            sendVoiceIcon = sendVoiceIcon,
            slideToCancelTextStyle = slideToCancelTextStyle,
            durationTextStyle = durationTextStyle,
            cancelTextStyle = cancelTextStyle,
            durationFormatter = durationFormatter
        ).let { styleCustomizer.apply(context, it) }
    }
}