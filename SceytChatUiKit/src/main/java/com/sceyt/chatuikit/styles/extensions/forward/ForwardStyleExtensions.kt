package com.sceyt.chatuikit.styles.extensions.forward

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.ForwardChannelItemStyle
import com.sceyt.chatuikit.styles.ForwardStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

internal fun ForwardStyle.Builder.buildSearchInputStyle(
        array: TypedArray
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

internal fun ForwardStyle.Builder.buildSearchToolbarStyle(
        array: TypedArray
) = SearchToolbarStyle(
    toolbarStyle = ToolbarStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
        underlineColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
        navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        ),
        titleTextStyle = TextStyle(
            color = context.getCompatColor(R.color.sceyt_color_text_primary),
            font = R.font.roboto_medium
        )
    ),
    searchInputStyle = buildSearchInputStyle(array)
)

internal fun ForwardStyle.Builder.buildActionButtonStyle(
        array: TypedArray
) = ButtonStyle(
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
        font = R.font.roboto_medium
    ),
    backgroundStyle = BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
        cornerRadius = 6.dpToPx().toFloat()
    )
)

internal fun ForwardStyle.Builder.buildChannelItemStyle(
        array: TypedArray
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
    avatarProvider = SceytChatUIKit.providers.channelDefaultAvatarProvider,
)