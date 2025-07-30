package com.sceyt.chatuikit.styles.edit

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
import com.sceyt.chatuikit.presentation.components.edit_channel.EditChannelFragment
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
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
 * @property avatarPlaceholder Placeholder for the avatar, default is [R.drawable.sceyt_ic_camera_72]
 * @property toolbarTitle Title for the toolbar, default is [R.string.sceyt_edit]
 * @property toolbarStyle Style for the toolbar
 * @property avatarStyle Style for the avatar
 * @property subjectTextInputStyle Style for the subject text input
 * @property aboutTextInputStyle Style for the about text input
 * @property uriTextInputStyle Style for the uri text input
 * @property saveButtonStyle Style for the save button
 * @property uriValidationStyle Style for the uri validation
 * */
data class EditChannelStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val dividerColor: Int,
        val avatarPlaceholder: Drawable?,
        val toolbarTitle: String,
        val toolbarStyle: ToolbarStyle,
        val avatarStyle: AvatarStyle,
        val subjectTextInputStyle: TextInputStyle,
        val aboutTextInputStyle: TextInputStyle,
        val uriTextInputStyle: TextInputStyle,
        val saveButtonStyle: ButtonStyle,
        val uriValidationStyle: URIValidationStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<EditChannelStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): EditChannelStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.EditChannel).use {
                val backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColor)
                val dividerColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor)
                val avatarPlaceholder = context.getCompatDrawable(R.drawable.sceyt_ic_camera_72).applyTint(
                    context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
                )
                val toolbarTitle = context.getString(R.string.sceyt_edit)
                val toolbarStyle = ToolbarStyle(
                    backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.primaryColor),
                    underlineColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor),
                    navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
                        context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
                    ),
                    titleTextStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                        font = R.font.roboto_medium
                    )
                )

                val avatarStyle = AvatarStyle(
                    avatarBackgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.overlayBackground2Color),
                )

                val subjectTextInputStyle = TextInputStyle(
                    textStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
                    ),
                    hintStyle = HintStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.textFootnoteColor),
                        hint = context.getString(R.string.sceyt_hint_channel_subject)
                    )
                )

                val aboutTextInputStyle = TextInputStyle(
                    textStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
                    ),
                    hintStyle = HintStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.textFootnoteColor),
                        hint = context.getString(R.string.sceyt_about)
                    )
                )

                val uriTextInputStyle = TextInputStyle(
                    textStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor)
                    ),
                    hintStyle = HintStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.textFootnoteColor),
                        hint = "sceytchn1"
                    )
                )

                val uriValidationStyle = URIValidationStyle(
                    successTextStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.successColor)
                    ),
                    errorTextStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKitTheme.colors.warningColor)
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
                    avatarPlaceholder = avatarPlaceholder,
                    toolbarTitle = toolbarTitle,
                    toolbarStyle = toolbarStyle,
                    avatarStyle = avatarStyle,
                    subjectTextInputStyle = subjectTextInputStyle,
                    aboutTextInputStyle = aboutTextInputStyle,
                    uriTextInputStyle = uriTextInputStyle,
                    uriValidationStyle = uriValidationStyle,
                    saveButtonStyle = saveButtonStyle
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}