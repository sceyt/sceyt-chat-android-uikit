package com.sceyt.chatuikit.styles.extensions.search

import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.search.ChatsSearchMessageItemStyle

internal fun ChatsSearchMessageItemStyle.Builder.buildSenderNameTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
}

internal fun ChatsSearchMessageItemStyle.Builder.buildMessageBodyTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textSecondaryColor),
        font = R.font.roboto_regular
    )
}

internal fun ChatsSearchMessageItemStyle.Builder.buildMetaTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textSecondaryColor),
        font = R.font.roboto_regular
    )
}

internal fun ChatsSearchMessageItemStyle.Builder.buildLastMessageSenderNameTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
}

internal fun ChatsSearchMessageItemStyle.Builder.buildMentionTextStyle(): TextStyle {
    val colors = SceytChatUIKit.theme.colors
    return TextStyle(
        color = context.getCompatColor(colors.textSecondaryColor),
        font = R.font.roboto_medium
    )
}

internal fun ChatsSearchMessageItemStyle.Builder.buildAvatarStyle(): AvatarStyle = AvatarStyle()
