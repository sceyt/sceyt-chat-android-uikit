@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.create_channel

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.create_channel.CreateChannelStyle
import com.sceyt.chatuikit.styles.cropper.ImageCropperStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.common.URIValidationStyle

internal fun CreateChannelStyle.Builder.buildCaptionTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
)

internal fun CreateChannelStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)

internal fun CreateChannelStyle.Builder.buildNameTextFieldStyle(
        array: TypedArray,
) = TextInputStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    ),
    hintStyle = HintStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
    )
)

internal fun CreateChannelStyle.Builder.buildAboutTextFieldStyle(
        array: TypedArray,
) = TextInputStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    ),
    hintStyle = HintStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
    )
)

internal fun CreateChannelStyle.Builder.buildUriTextFieldStyle(
        array: TypedArray,
) = TextInputStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    ),
    hintStyle = HintStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
    )
)


internal fun CreateChannelStyle.Builder.buildUriValidationStyle(
        array: TypedArray,
) = URIValidationStyle(
    successTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.successColor),
    ),
    errorTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.warningColor),
    ),
    messageProvider = SceytChatUIKit.providers.channelURIValidationMessageProvider
)

internal fun CreateChannelStyle.Builder.buildActionButtonStyle(
        array: TypedArray,
) = ButtonStyle(
    backgroundStyle = BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
    ),
    icon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_next).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
)

internal fun CreateChannelStyle.Builder.buildImageCropperStyle(
        array: TypedArray,
) = ImageCropperStyle.default(context)
