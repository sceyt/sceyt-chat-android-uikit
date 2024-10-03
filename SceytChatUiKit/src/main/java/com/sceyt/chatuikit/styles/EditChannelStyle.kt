package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.edit_channel.EditChannelFragment
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.common.URIValidationStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [EditChannelFragment]
 * @property backgroundColor Background color of the fragment, default is [Colors.backgroundColor]
 * @property dividerColor Color of the divider, default is [Colors.borderColor]
 * @property avatarBackgroundColor Background color of the avatar, default is [Colors.overlayBackground2Color]
 * @property avatarPlaceholder Placeholder for the avatar, default is [R.drawable.sceyt_ic_camera_72]
 * @property toolbarStyle Style for the toolbar
 * @property subjectTextInputStyle Style for the subject text input
 * @property aboutTextInputStyle Style for the about text input
 * @property uriTextInputStyle Style for the uri text input
 * @property saveButtonStyle Style for the save button
 * @property uriValidationStyle Style for the uri validation
 * */
data class EditChannelStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val dividerColor: Int,
        @ColorInt val avatarBackgroundColor: Int,
        val avatarPlaceholder: Drawable?,
        val toolbarStyle: ToolbarStyle,
        val subjectTextInputStyle: TextInputStyle,
        val aboutTextInputStyle: TextInputStyle,
        val uriTextInputStyle: TextInputStyle,
        val saveButtonStyle: ButtonStyle,
        val uriValidationStyle: URIValidationStyle,
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): EditChannelStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)
            val dividerColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor)
            val avatarBackgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.overlayBackground2Color)
            val avatarPlaceholder = context.getCompatDrawable(R.drawable.sceyt_ic_camera_72).applyTint(
                context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
            )
            val toolbarStyle = ToolbarStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.primaryColor),
                borderColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor),
                navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
                ),
                titleTextStyle = TextStyle(
                    color = context.getCompatColor(R.color.sceyt_color_text_primary),
                    font = R.font.roboto_medium
                )
            )

            val subjectTextInputStyle = TextInputStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
                ),
                hintStyle = HintStyle(
                    textColor = context.getCompatColor(SceytChatUIKitTheme.colors.textFootnoteColor),
                    hint = context.getString(R.string.sceyt_hint_channel_subject)
                )
            )

            val aboutTextInputStyle = TextInputStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
                ),
                hintStyle = HintStyle(
                    textColor = context.getCompatColor(SceytChatUIKitTheme.colors.textFootnoteColor),
                    hint = context.getString(R.string.sceyt_about)
                )
            )

            val uriTextInputStyle = TextInputStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
                ),
                hintStyle = HintStyle(
                    textColor = context.getCompatColor(SceytChatUIKitTheme.colors.textFootnoteColor),
                    hint = "sceytchn1"
                )
            )

            val uriValidationStyle = URIValidationStyle(
                successTextStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKitTheme.colors.successColor)
                ),
                errorTextStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKitTheme.colors.errorColor)
                ),
                messageProvider = SceytChatUIKit.providers.channelURIValidationMessageProvider
            )

            val saveButtonStyle = ButtonStyle(
                backgroundStyle = BackgroundStyle(
                    backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor),
                ),
                icon = context.getCompatDrawable(R.drawable.sceyt_ic_save).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                )
            )

            return EditChannelStyle(
                backgroundColor = backgroundColor,
                dividerColor = dividerColor,
                avatarBackgroundColor = avatarBackgroundColor,
                avatarPlaceholder = avatarPlaceholder,
                toolbarStyle = toolbarStyle,
                subjectTextInputStyle = subjectTextInputStyle,
                aboutTextInputStyle = aboutTextInputStyle,
                uriTextInputStyle = uriTextInputStyle,
                uriValidationStyle = uriValidationStyle,
                saveButtonStyle = saveButtonStyle
            )
        }
    }
}