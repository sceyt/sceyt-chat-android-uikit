package com.sceyt.chatuikit.styles.media_picker

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [BottomSheetMediaPicker] media item.
 * @property backgroundColor Background color of the media picker, default is [Colors.backgroundColor].
 * @property videoDurationIcon Icon to be displayed for video duration, default is [R.drawable.sceyt_ic_video].
 * @property brokenMediaPlaceHolder Drawable to be displayed when media is broken, default is [R.drawable.sceyt_ic_broken_image].
 * @property videoDurationTextStyle Style for the video duration text..
 * @property checkboxStyle Style for the selection checkbox.
 * @property mediaDurationFormatter Formatter for the media duration.
 * */
data class MediaPickerItemStyle(
        @ColorInt val backgroundColor: Int,
        val videoDurationIcon: Drawable?,
        val brokenMediaPlaceHolder: Drawable?,
        val videoDurationTextStyle: TextStyle,
        val checkboxStyle: CheckboxStyle,
        val mediaDurationFormatter: Formatter<Long>,
) {
    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MediaPickerItemStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): MediaPickerItemStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.MediaPicker).use {
                val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColorSecondary)

                val checkboxStyle = CheckboxStyle(
                    checkedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state_with_layers).applyTintBackgroundLayer(
                        context.getCompatColor(SceytChatUIKitTheme.colors.accentColor), R.id.backgroundLayer
                    ),
                    uncheckedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_unchecked_state_picker)
                )

                val videoDurationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_video)

                val brokenMediaPlaceHolder = context.getCompatDrawable(R.drawable.sceyt_ic_broken_image)

                val videoDurationTextStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
                )

                return MediaPickerItemStyle(
                    backgroundColor = backgroundColor,
                    videoDurationIcon = videoDurationIcon,
                    brokenMediaPlaceHolder = brokenMediaPlaceHolder,
                    videoDurationTextStyle = videoDurationTextStyle,
                    checkboxStyle = checkboxStyle,
                    mediaDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}