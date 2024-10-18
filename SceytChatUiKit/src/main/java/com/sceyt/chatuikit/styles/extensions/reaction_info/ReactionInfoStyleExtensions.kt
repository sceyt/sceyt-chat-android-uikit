@file:Suppress("UNUSED_PARAMETER", "UnusedReceiverParameter")

package com.sceyt.chatuikit.styles.extensions.reaction_info

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.spToPx
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.reactions_info.ReactedUserItemStyle
import com.sceyt.chatuikit.styles.reactions_info.ReactedUserListStyle
import com.sceyt.chatuikit.styles.reactions_info.ReactionsInfoHeaderItemStyle
import com.sceyt.chatuikit.styles.reactions_info.ReactionsInfoStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme


internal fun ReactionsInfoStyle.Builder.buildBackgroundStyle(
        array: TypedArray
): BackgroundStyle {
    val cornersRadius = 16.dpToPx().toFloat()
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.backgroundColorSections),
        shape = Shape.RoundedCornerShape(topLeft = cornersRadius, topRight = cornersRadius),
    )
}

internal fun ReactionsInfoStyle.Builder.buildHeaderBackgroundStyle(
        array: TypedArray
): BackgroundStyle {
    return BackgroundStyle()
}

internal fun ReactionsInfoHeaderItemStyle.Builder.buildTextStyle(
        array: TypedArray
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKitTheme.colors.textPrimaryColor),
    font = R.font.roboto_medium,
    size = 15.spToPx()
)

internal fun ReactionsInfoHeaderItemStyle.Builder.buildSelectedTextStyle(
        array: TypedArray
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor),
    font = R.font.roboto_medium,
    size = 15.spToPx()
)

internal fun ReactedUserListStyle.Builder.buildUserItemStyle(
        array: TypedArray
) = ReactedUserItemStyle(
    titleTextStyle = TextStyle(color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)),
    titleFormatter = SceytChatUIKit.formatters.userNameFormatter,
    subtitleFormatter = Formatter { _, it -> it },
    avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer
)