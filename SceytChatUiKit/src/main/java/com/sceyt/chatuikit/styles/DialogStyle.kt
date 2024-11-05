package com.sceyt.chatuikit.styles

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.createColorState
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle

data class DialogStyle(
        val backgroundStyle: BackgroundStyle,
        val titleStyle: TextStyle,
        val subtitleStyle: TextStyle,
        val positiveButtonStyle: ButtonStyle,
        val negativeButtonStyle: ButtonStyle,
        val optionButtonStyle: ButtonStyle,
        val warningOptionButtonStyle: ButtonStyle,
        val checkboxStyle: CheckboxStyle,
) {
    companion object {

        var styleCustomizer = StyleCustomizer<DialogStyle> { _, style -> style }

        fun default(context: Context) = DialogStyle(
            backgroundStyle = BackgroundStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
                shape = Shape.RoundedCornerShape(12.dpToPx().toFloat()),
            ),
            titleStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            ),
            subtitleStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
            ),
            positiveButtonStyle = ButtonStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
                    font = R.font.roboto_medium
                )
            ),
            negativeButtonStyle = ButtonStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
                    font = R.font.roboto_medium
                )
            ),
            optionButtonStyle = ButtonStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                    drawableColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )
            ),
            warningOptionButtonStyle = ButtonStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.warningColor),
                    drawableColor = context.getCompatColor(SceytChatUIKit.theme.colors.warningColor)
                )
            ),
            checkboxStyle = CheckboxStyle(
                textStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
                ),
                buttonTint = context.createColorState()
            )
        ).let { styleCustomizer.apply(context, it) }
    }
}