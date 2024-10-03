package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
/**
 * Style for [BottomSheetMediaPicker].
 * @property backgroundColor Background color of the media picker, default is [Colors.backgroundColor].
 * @property mediaBackgroundColor Background color of the media items, default is [Colors.backgroundColorSecondary].
 * @property titleText Text to be displayed as the title of the media picker, default is "Gallery".
 * @property selectionCheckboxStyle Style for the selection checkbox, default is [CheckboxStyle].
 * @property videoDurationIcon Icon to be displayed for video duration, default is [R.drawable.sceyt_ic_video].
 * @property brokenMediaPlaceHolder Drawable to be displayed when media is broken, default is [R.drawable.sceyt_ic_broken_image].
 * @property titleTextStyle Style for the title text.
 * @property videoDurationTextStyle Style for the video duration text..
 * @property countTextStyle Style for the count of selected items.
 * @property countBackgroundStyle Style for the count of selected items.
 * @property confirmButtonStyle Style for the confirm button.
 * @property mediaDurationFormatter Formatter for the media duration.
 * */
data class MediaPickerStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val mediaBackgroundColor: Int,
        val titleText: String,
        val selectionCheckboxStyle: CheckboxStyle,
        val videoDurationIcon: Drawable?,
        val brokenMediaPlaceHolder: Drawable?,
        val titleTextStyle: TextStyle,
        val videoDurationTextStyle: TextStyle,
        val countTextStyle: TextStyle,
        val countBackgroundStyle: BackgroundStyle,
        val confirmButtonStyle: ButtonStyle,
        val mediaDurationFormatter: Formatter<Long>
) {

    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MediaPickerStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): MediaPickerStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)
            val mediaBackgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColorSecondary)
            val titleText = context.getString(R.string.sceyt_gallery)

            val selectionCheckboxStyle = CheckboxStyle(
                checkedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state_with_layers).applyTintBackgroundLayer(
                    context.getCompatColor(SceytChatUIKitTheme.colors.accentColor), R.id.backgroundLayer
                ),
                uncheckedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_gallery_unchecked_state)
            )

            val videoDurationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_video)

            val brokenMediaPlaceHolder = context.getCompatDrawable(R.drawable.sceyt_ic_broken_image)

            val videoDurationTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
            )

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            val countTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
            )

            val countBackgroundStyle = BackgroundStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor),
                shape = GradientDrawable.RECTANGLE,
                cornerRadius = 30f,
                borderWidth = 5,
                borderColor = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
            )

            val confirmButtonStyle = ButtonStyle(
                icon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_next).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                ),
                backgroundStyle = BackgroundStyle(
                    backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor),
                    shape = GradientDrawable.OVAL,
                )
            )

            return MediaPickerStyle(
                backgroundColor = backgroundColor,
                mediaBackgroundColor = mediaBackgroundColor,
                titleText = titleText,
                selectionCheckboxStyle = selectionCheckboxStyle,
                videoDurationIcon = videoDurationIcon,
                brokenMediaPlaceHolder = brokenMediaPlaceHolder,
                titleTextStyle = titleTextStyle,
                videoDurationTextStyle = videoDurationTextStyle,
                countTextStyle = countTextStyle,
                countBackgroundStyle = countBackgroundStyle,
                confirmButtonStyle = confirmButtonStyle,
                mediaDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}