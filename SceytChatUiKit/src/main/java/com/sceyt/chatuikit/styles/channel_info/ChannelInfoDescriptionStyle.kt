package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoDescriptionStyle(
        @ColorInt var backgroundColor: Int,
        var titleText: String,
        var titleTextStyle: TextStyle,
        var descriptionTextStyle: TextStyle
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoDescriptionStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val titleText = context.getString(R.string.sceyt_about)

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textSecondaryColor),
                font = R.font.roboto_medium
            )

            val descriptionTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
            )

            return ChannelInfoDescriptionStyle(
                backgroundColor = backgroundColor,
                titleText = titleText,
                titleTextStyle = titleTextStyle,
                descriptionTextStyle = descriptionTextStyle
            )
        }
    }
}
