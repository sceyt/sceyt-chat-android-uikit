@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.share

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.forward.ForwardChannelItemStyle
import com.sceyt.chatuikit.styles.share.ShareStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

internal fun ShareStyle.Builder.buildMessageInputStyle(
        array: TypedArray,
) = TextInputStyle(
    backgroundStyle = BackgroundStyle(
        background = context.getCompatDrawable(R.drawable.sceyt_bg_top_bottom_lines).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
        ),
    ),
    hintStyle = HintStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
        hint = context.getString(R.string.sceyt_write_a_message)
    ),
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
)

internal fun ShareStyle.Builder.buildSearchInputStyle(
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
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
            hint = context.getString(R.string.sceyt_search)
        ),
        textStyle = TextStyle(
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
        )
    )
)

internal fun ShareStyle.Builder.buildSearchToolbarStyle(
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

internal fun ShareStyle.Builder.buildActionButtonStyle(
        array: TypedArray,
) = ButtonStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
        font = R.font.roboto_medium
    ),
    backgroundStyle = BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
        shape = Shape.RoundedCornerShape(6.dpToPx().toFloat())
    )
)

internal fun ShareStyle.Builder.buildChannelItemStyle(
        array: TypedArray,
) = ForwardChannelItemStyle(
    dividerColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
    checkboxStyle = CheckboxStyle.default(context),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    ),
    subtitleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    ),
    titleFormatter = SceytChatUIKit.formatters.channelNameFormatter,
    subtitleFormatter = SceytChatUIKit.formatters.channelSubtitleFormatter,
    avatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer,
)