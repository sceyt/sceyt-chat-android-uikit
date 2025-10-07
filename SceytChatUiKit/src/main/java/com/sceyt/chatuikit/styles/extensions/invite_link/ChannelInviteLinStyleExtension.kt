@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.invite_link

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.SwitchStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.invite_link.ChannelInviteLinkStyle

internal fun ChannelInviteLinkStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)

internal fun ChannelInviteLinkStyle.Builder.buildInviteLinkTitleTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun ChannelInviteLinkStyle.Builder.buildInviteLinkTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    font = R.font.roboto_regular
)


internal fun ChannelInviteLinkStyle.Builder.buildShowPreviewMessagesSwitchStyle(
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

internal fun ChannelInviteLinkStyle.Builder.buildShowPreviewMessagesSubtitleTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_regular
)

internal fun ChannelInviteLinkStyle.Builder.buildOptionsTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    font = R.font.roboto_regular
)

internal fun ChannelInviteLinkStyle.Builder.buildLinkPreviewBackgroundStyle(
        array: TypedArray,
) = BackgroundStyle(
    background = context.getCompatDrawable(R.drawable.sceyt_bg_ripple_corners_8).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color),
    )
)