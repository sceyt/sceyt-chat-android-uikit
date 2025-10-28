@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.create_poll

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.SwitchStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle

internal fun CreatePollStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)

internal fun CreatePollStyle.Builder.buildQuestionTitleTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun CreatePollStyle.Builder.buildQuestionInputTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    font = R.font.roboto_regular
)

internal fun CreatePollStyle.Builder.buildOptionsTitleTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun CreatePollStyle.Builder.buildOptionInputTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    font = R.font.roboto_regular
)

internal fun CreatePollStyle.Builder.buildAddOptionTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
    font = R.font.roboto_medium
)

internal fun CreatePollStyle.Builder.buildParametersTitleTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun CreatePollStyle.Builder.buildParametersSwitchStyle(
        array: TypedArray,
) = SwitchStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_regular
    ),
    checkedColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
    thumbUncheckedColor = context.getCompatColor(R.color.sceyt_switch_thumb_unchecked_color),
    trackUncheckedColor = context.getCompatColor(R.color.sceyt_switch_track_unchecked_color)
)

internal fun CreatePollStyle.Builder.buildQuestionBackgroundStyle(
        array: TypedArray,
) = BackgroundStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color),
    shape = Shape.RoundedCornerShape(8f.dpToPx()),
)

internal fun CreatePollStyle.Builder.buildOptionItemBackgroundStyle(
        array: TypedArray,
) = BackgroundStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color),
    shape = Shape.RoundedCornerShape(8f.dpToPx()),
)

