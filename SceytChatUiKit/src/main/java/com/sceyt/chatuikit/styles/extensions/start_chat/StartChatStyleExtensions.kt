@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.start_chat

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.StartChatStyle

internal fun StartChatStyle.Builder.buildCreateGroupTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
)

internal fun StartChatStyle.Builder.buildCreateChannelTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
)

internal fun StartChatStyle.Builder.buildSeparatorTextStyle(
        array: TypedArray,
) = TextStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color),
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun StartChatStyle.Builder.buildSearchInputStyle(
        array: TypedArray,
) = SearchInputStyle(
    searchIcon = context.getCompatDrawable(R.drawable.sceyt_ic_search).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    ),
    clearIcon = context.getCompatDrawable(R.drawable.sceyt_ic_cancel).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
    ),
    textInputStyle = TextInputStyle(
        hintStyle = HintStyle(
            textColor = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
            hint = context.getString(R.string.sceyt_search)
        ),
        textStyle = TextStyle(
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
        )
    )
)

internal fun StartChatStyle.Builder.buildSearchToolbarStyle(
        array: TypedArray,
) = SearchToolbarStyle(
    toolbarStyle = ToolbarStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
        underlineColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
        navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        ),
        titleTextStyle = TextStyle(
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
            font = R.font.roboto_medium
        )
    ),
    searchInputStyle = buildSearchInputStyle(array)
)

internal fun StartChatStyle.Builder.buildItemStyle(
        array: TypedArray,
): ListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, AvatarRenderer<SceytUser>> {

    val titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
    val subtitleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )

    return ListItemStyle(
        titleTextStyle = titleTextStyle,
        subtitleTextStyle = subtitleTextStyle,
        titleFormatter = SceytChatUIKit.formatters.userNameFormatter,
        subtitleFormatter = SceytChatUIKit.formatters.userPresenceDateFormatter,
        avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer
    )
}