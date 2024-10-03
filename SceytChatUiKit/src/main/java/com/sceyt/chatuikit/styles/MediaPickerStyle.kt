package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class MediaPickerStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val mediaBackgroundColor: Int,
        val selectionCheckboxStyle: CheckboxStyle,
        val videoDurationIcon: Drawable?,
        val brokenMediaPlaceHolder: Drawable?,
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

            val selectionCheckboxStyle = CheckboxStyle(
                checkedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
                ),
                uncheckedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_gallery_unchecked_state)
            )

            val videoDurationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_video)

            val brokenMediaPlaceHolder = context.getCompatDrawable(R.drawable.sceyt_ic_broken_image)

            val videoDurationTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
            )

            val countTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
            )

            val countBackgroundStyle = BackgroundStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor),
                shape = GradientDrawable.OVAL,
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
                selectionCheckboxStyle = selectionCheckboxStyle,
                videoDurationIcon = videoDurationIcon,
                brokenMediaPlaceHolder = brokenMediaPlaceHolder,
                videoDurationTextStyle = videoDurationTextStyle,
                countTextStyle = countTextStyle,
                countBackgroundStyle = countBackgroundStyle,
                confirmButtonStyle = confirmButtonStyle,
                mediaDurationFormatter = SceytChatUIKit.formatters.mediaDurationFormatter
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}