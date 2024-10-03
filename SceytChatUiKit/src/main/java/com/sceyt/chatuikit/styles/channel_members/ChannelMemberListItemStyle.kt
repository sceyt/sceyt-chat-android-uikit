package com.sceyt.chatuikit.styles.channel_members

import android.content.Context
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class ChannelMemberListItemStyle(
        var roleTextStyle: TextStyle,
        var listItemStyle: ListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, VisualProvider<SceytUser, DefaultAvatar>>
) {

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {

        fun build(): ChannelMemberListItemStyle {

            val roleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
                font = R.font.roboto_medium
            )

            val listItemStyle = ListItemStyle(
                titleTextStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                    font = R.font.roboto_medium
                ),
                subtitleTextStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
                ),
                titleFormatter = SceytChatUIKit.formatters.userNameFormatter,
                subtitleFormatter = SceytChatUIKit.formatters.userPresenceDateFormatter,
                avatarProvider = SceytChatUIKit.providers.userDefaultAvatarProvider
            )

            return ChannelMemberListItemStyle(
                roleTextStyle = roleTextStyle,
                listItemStyle = listItemStyle
            )
        }
    }
}