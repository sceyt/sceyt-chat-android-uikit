package com.sceyt.chatuikit.styles.channel_members

import android.content.Context
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.UserAvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class ChannelMemberListItemStyle(
        val roleTextStyle: TextStyle,
        val listItemStyle: ListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, UserAvatarRenderer>
) {

    companion object {
        var styleCustomizer = StyleCustomizer<ChannelMemberListItemStyle> { _, style -> style }
    }

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
                avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer
            )

            return ChannelMemberListItemStyle(
                roleTextStyle = roleTextStyle,
                listItemStyle = listItemStyle
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}