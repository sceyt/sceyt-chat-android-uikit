package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.renderers.UserAvatarRenderer
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMessageBodyTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMetaTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSenderNameTextStyle
import java.util.Date

data class ChatsSearchMessageItemStyle(
    val senderNameTextStyle: TextStyle,
    val messageBodyTextStyle: TextStyle,
    val metaTextStyle: TextStyle,
    @param:ColorInt val highlightTextColor: Int,
    val avatarStyle: AvatarStyle,
    val senderNameFormatter: Formatter<SceytUser>,
    val messageDateFormatter: Formatter<Date>,
    val messageBodyFormatter: Formatter<MessageBodyFormatterAttributes>,
    val channelNameFormatter: Formatter<SceytChannel>,
    val userAvatarRenderer: UserAvatarRenderer,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChatsSearchMessageItemStyle> { _, style -> style }
    }

    internal class Builder(internal val context: Context) {
        fun build(): ChatsSearchMessageItemStyle {
            val colors = SceytChatUIKit.theme.colors
            return ChatsSearchMessageItemStyle(
                senderNameTextStyle = buildSenderNameTextStyle(),
                messageBodyTextStyle = buildMessageBodyTextStyle(),
                metaTextStyle = buildMetaTextStyle(),
                highlightTextColor = context.getCompatColor(colors.accentColor),
                avatarStyle = buildAvatarStyle(),
                senderNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                messageDateFormatter = SceytChatUIKit.formatters.messageDateFormatter,
                messageBodyFormatter = SceytChatUIKit.formatters.messageBodyFormatter,
                channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                userAvatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer,
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
