package com.sceyt.chatuikit.styles.media_picker

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [BottomSheetMediaPicker].
 * @property backgroundColor Background color of the media picker, default is [Colors.backgroundColor].
 * @property titleText Text to be displayed as the title of the media picker, default is [R.string.sceyt_gallery]".
 * @property titleTextStyle Style for the title text.
 * @property confirmButtonStyle Style for the confirm button.
 * @property countTextStyle Style for the count of selected items.
 * @property countBackgroundStyle Style for the count of selected items.
 * @property itemStyle Style for the media item.
 * @property emptyStateStyle Style for the empty state shown when the gallery has no photos or videos.
 * */
data class MediaPickerStyle(
        @param:ColorInt val backgroundColor: Int,
        val titleText: String,
        val titleTextStyle: TextStyle,
        val countTextStyle: TextStyle,
        val confirmButtonStyle: ButtonStyle,
        val countBackgroundStyle: BackgroundStyle,
        val itemStyle: MediaPickerItemStyle,
        val emptyStateStyle: EmptyStateStyle,
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
            val titleText = context.getString(R.string.sceyt_gallery)

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            val confirmButtonStyle = ButtonStyle(
                icon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_next).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                ),
                backgroundStyle = BackgroundStyle(
                    backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor),
                    shape = Shape.Circle,
                )
            )

            val countTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
            )

            val countBackgroundStyle = BackgroundStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor),
                shape = Shape.RoundedCornerShape(30f),
                borderWidth = 5,
                borderColor = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
            )

            val itemStyle = MediaPickerItemStyle.Builder(context, attributeSet).build()

            val emptyStateStyle = buildEmptyStateStyle(
                context = context,
                iconRes = R.drawable.sceyt_ic_gallery,
                titleText = context.getString(R.string.sceyt_no_gallery_media_title),
                subtitleText = context.getString(R.string.sceyt_no_gallery_media_desc)
            )

            return MediaPickerStyle(
                backgroundColor = backgroundColor,
                titleText = titleText,
                titleTextStyle = titleTextStyle,
                confirmButtonStyle = confirmButtonStyle,
                countTextStyle = countTextStyle,
                countBackgroundStyle = countBackgroundStyle,
                itemStyle = itemStyle,
                emptyStateStyle = emptyStateStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}