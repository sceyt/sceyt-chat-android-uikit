package com.sceyt.chatuikit.styles.extensions.pinned_messages

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setIconsTintColorRes
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle

internal fun PinnedMessagesStyle.Builder.buildToolbarStyle(array: TypedArray) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
    underlineColor = array.getColor(
        R.styleable.PinnedMessages_sceytUiToolbarDividerColor,
        context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    ),
    navigationIcon = array.getDrawable(R.styleable.PinnedMessages_sceytUiToolbarNavigationIcon)
        ?: context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back)
            .applyTint(context, SceytChatUIKit.theme.colors.accentColor),
    titleTextStyle = TextStyle.Builder(array)
        .setColor(
            R.styleable.PinnedMessages_sceytUiToolbarTitleTextColor,
            context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
        )
        .setFont(R.styleable.PinnedMessages_sceytUiPinnedMessagesTitleTextFont, R.font.roboto_medium)
        .setSize(
            R.styleable.PinnedMessages_sceytUiPinnedMessagesTitleTextSize,
            context.resources.getDimensionPixelSize(R.dimen.bigTextSize)
        )
        .build()
)

internal fun PinnedMessagesStyle.Builder.buildNavigateIcon(array: TypedArray) =
    array.getDrawable(R.styleable.PinnedMessages_sceytUiPinnedMessagesNavigateIcon)
        ?: context.getCompatDrawable(R.drawable.sceyt_ic_pinned_message_navigate)
            .applyTint(context, SceytChatUIKit.theme.colors.iconSecondaryColor)

internal fun PinnedMessagesStyle.Builder.buildNavigateButtonBackgroundStyle(
    array: TypedArray,
) = BackgroundStyle(
    backgroundColor = array.getColor(
        R.styleable.PinnedMessages_sceytUiPinnedMessagesNavigateBackgroundColor,
        context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    ),
    shape = Shape.Circle
)

internal fun PinnedMessagesStyle.Builder.buildEmptyStateTextStyle(array: TypedArray) =
    TextStyle.Builder(array)
        .setColor(
            R.styleable.PinnedMessages_sceytUiPinnedMessagesEmptyTextColor,
            context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
        )
        .setSize(
            R.styleable.PinnedMessages_sceytUiPinnedMessagesEmptyTextSize,
            context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
        )
        .build()

internal fun PinnedMessagesStyle.Builder.buildMessageActionsMenuStyle() = MenuStyle(
    popupTheme = R.style.SceytPopupMenuStyle,
    titleAppearance = R.style.SceytMenuTitleAppearance,
    menuRes = R.menu.sceyt_menu_message_actions,
    overFlowIcon = context.getCompatDrawable(R.drawable.sceyt_ic_more_24)
        .applyTint(context, SceytChatUIKit.theme.colors.accentColor),
    menuCustomizer = {
        setIconsTintColorRes(context, SceytChatUIKit.theme.colors.accentColor)
    }
)
