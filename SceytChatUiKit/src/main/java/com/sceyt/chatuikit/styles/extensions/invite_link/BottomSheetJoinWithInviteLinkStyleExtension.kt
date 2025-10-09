@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.invite_link

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.invite_link.BottomSheetJoinWithInviteLinkStyle

internal fun BottomSheetJoinWithInviteLinkStyle.Builder.buildBackgroundStyle(
        array: TypedArray,
): BackgroundStyle {
    val cornersRadius = 16.dpToPx().toFloat()
    return BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColorSections),
        shape = Shape.RoundedCornerShape(topLeft = cornersRadius, topRight = cornersRadius),
    )
}

internal fun BottomSheetJoinWithInviteLinkStyle.Builder.buildJoinSubjectTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    font = R.font.roboto_medium
)

internal fun BottomSheetJoinWithInviteLinkStyle.Builder.buildJoinSubtitleTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_regular
)

internal fun BottomSheetJoinWithInviteLinkStyle.Builder.buildMemberNamesTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_regular
)

internal fun BottomSheetJoinWithInviteLinkStyle.Builder.buildJoinButtonStyle(
        array: TypedArray,
) = ButtonStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
        font = R.font.roboto_regular
    ),
    backgroundStyle = com.sceyt.chatuikit.styles.common.BackgroundStyle(
        background = context.getCompatDrawable(R.drawable.sceyt_bg_corners_8)?.applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        )
    )
)

