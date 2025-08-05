@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.create_group

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.create_channel.CreateGroupStyle
import com.sceyt.chatuikit.styles.create_channel.CreateGroupStyleUserItemStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

internal fun CreateGroupStyle.Builder.buildSeparatorTextStyle(
        array: TypedArray,
) = TextStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color),
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun CreateGroupStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)

internal fun CreateGroupStyle.Builder.buildNameTextFieldStyle(
        array: TypedArray,
) = TextInputStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    ),
    hintStyle = HintStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
    )
)

internal fun CreateGroupStyle.Builder.buildAboutTextFieldStyle(
        array: TypedArray,
) = TextInputStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    ),
    hintStyle = HintStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
    )
)

internal fun CreateGroupStyle.Builder.buildActionButtonStyle(
        array: TypedArray,
) = ButtonStyle(
    backgroundStyle = BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
    ),
    icon = context.getCompatDrawable(R.drawable.sceyt_ic_save).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
)

internal fun CreateGroupStyle.Builder.buildUserItemStyle(
        array: TypedArray,
) = CreateGroupStyleUserItemStyle(
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    ),
    subtitleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    ),
    avatarStyle = AvatarStyle(),
    titleFormatter = SceytChatUIKit.formatters.userNameFormatter,
    subtitleFormatter = SceytChatUIKit.formatters.userPresenceDateFormatter,
    avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer

)
