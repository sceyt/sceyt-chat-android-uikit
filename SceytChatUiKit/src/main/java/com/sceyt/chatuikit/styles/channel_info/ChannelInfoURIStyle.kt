package com.sceyt.chatuikit.styles.channel_info

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

data class ChannelInfoURIStyle(
        @ColorInt val backgroundColor: Int,
        val uriIcon: Drawable?,
        val titleTextStyle: TextStyle
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelInfoURIStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections)

            val uriIcon = context.getCompatDrawable(R.drawable.sceyt_ic_channel_uri_with_layers)
                ?.applyTintBackgroundLayer(context, SceytChatUIKitTheme.colors.accentColor3, R.id.backgroundLayer)

            val titleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            return ChannelInfoURIStyle(
                backgroundColor = backgroundColor,
                uriIcon = uriIcon,
                titleTextStyle = titleTextStyle
            )
        }
    }
}
