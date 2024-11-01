package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.create_chat.create_channel.CreateChannelActivity
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.common.URIValidationStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildAboutTextFieldStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildActionButtonStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildCaptionTextStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildNameTextFieldStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildToolbarStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildUriTextFieldStyle
import com.sceyt.chatuikit.styles.extensions.create_channel.buildUriValidationStyle
import com.sceyt.chatuikit.theme.Colors

/**
 * Style for the [CreateChannelActivity] page.
 * @param backgroundColor Background color of the page. Default is [Colors.backgroundColor].
 * @param avatarBackgroundColor Background color of the avatar. Default is [Colors.overlayBackground2Color].
 * @param dividerColor Color of the dividers. Default is [Colors.borderColor].
 * @param avatarDefaultIcon Default icon for the avatar. Default is [R.drawable.sceyt_ic_camera_72].
 * @param captionTextStyle Style for the caption text. Default is [buildCaptionTextStyle].
 * @param toolbarStyle Style for the toolbar. Default is [buildToolbarStyle].
 * @param nameTextFieldStyle Style for the name text field. Default is [buildNameTextFieldStyle].
 * @param aboutTextFieldStyle Style for the about text field. Default is [buildAboutTextFieldStyle].
 * @param uriTextFieldStyle Style for the URI text field. Default is [buildUriTextFieldStyle].
 * @param uriValidationStyle Style for the URI validation. Default is [buildUriValidationStyle].
 * @param actionButtonStyle Style for the action button. Default is [buildActionButtonStyle].
 * */
data class CreateChannelStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val avatarBackgroundColor: Int,
        @ColorInt val dividerColor: Int,
        val avatarDefaultIcon: Drawable?,
        val captionTextStyle: TextStyle,
        val toolbarStyle: ToolbarStyle,
        val nameTextFieldStyle: TextInputStyle,
        val aboutTextFieldStyle: TextInputStyle,
        val uriTextFieldStyle: TextInputStyle,
        val uriValidationStyle: URIValidationStyle,
        val actionButtonStyle: ButtonStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<CreateChannelStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): CreateChannelStyle {
            context.obtainStyledAttributes(attrs, R.styleable.CreateChannel).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                val avatarBackgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackground2Color)
                val dividerColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
                val avatarDefaultIcon = context.getCompatDrawable(R.drawable.sceyt_ic_camera_72).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                )

                return CreateChannelStyle(
                    backgroundColor = backgroundColor,
                    avatarBackgroundColor = avatarBackgroundColor,
                    dividerColor = dividerColor,
                    avatarDefaultIcon = avatarDefaultIcon,
                    captionTextStyle = buildCaptionTextStyle(array),
                    toolbarStyle = buildToolbarStyle(array),
                    nameTextFieldStyle = buildNameTextFieldStyle(array),
                    aboutTextFieldStyle = buildAboutTextFieldStyle(array),
                    uriTextFieldStyle = buildUriTextFieldStyle(array),
                    uriValidationStyle = buildUriValidationStyle(array),
                    actionButtonStyle = buildActionButtonStyle(array),
                )
            }
        }
    }
}
