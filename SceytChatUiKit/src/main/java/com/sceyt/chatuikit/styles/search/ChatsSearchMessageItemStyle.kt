package com.sceyt.chatuikit.styles.search

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.SearchMessageResultFormatterAttributes
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.renderers.ChannelAvatarRenderer
import com.sceyt.chatuikit.renderers.UserAvatarRenderer
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.search.buildLastMessageSenderNameTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMentionTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMessageBodyTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMetaTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSenderNameTextStyle
import java.util.Date

data class ChatsSearchMessageItemStyle(
    @param:ColorInt val highlightTextColor: Int,
    val titleTextStyle: TextStyle,
    val messageBodyTextStyle: TextStyle,
    val lastMessageSenderNameTextStyle: TextStyle,
    val dateTextStyle: TextStyle,
    val mentionTextStyle: TextStyle,
    val avatarStyle: AvatarStyle,
    val userNameFormatter: Formatter<SceytUser>,
    val senderNameFormatter: Formatter<SceytMessage>,
    val messageDateFormatter: Formatter<Date>,
    val messageBodyFormatter: Formatter<MessageBodyFormatterAttributes>,
    val mentionUserNameFormatter: Formatter<SceytUser>,
    val channelNameFormatter: Formatter<SceytChannel>,
    val attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?>,
    val searchMessageResultBodyFormatter: Formatter<SearchMessageResultFormatterAttributes>,
    val userAvatarRenderer: UserAvatarRenderer,
    val channelAvatarRenderer: ChannelAvatarRenderer,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ChatsSearchMessageItemStyle> { _, style -> style }
    }

    internal class Builder(internal val context: Context) {
        fun build(): ChatsSearchMessageItemStyle {
            val colors = SceytChatUIKit.theme.colors
            return ChatsSearchMessageItemStyle(
                highlightTextColor = context.getCompatColor(colors.textPrimaryColor),
                titleTextStyle = buildSenderNameTextStyle(),
                messageBodyTextStyle = buildMessageBodyTextStyle(),
                lastMessageSenderNameTextStyle = buildLastMessageSenderNameTextStyle(),
                dateTextStyle = buildMetaTextStyle(),
                mentionTextStyle = buildMentionTextStyle(),
                avatarStyle = buildAvatarStyle(),
                userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                senderNameFormatter = SceytChatUIKit.formatters.searchMessageSenderNameFormatter,
                messageDateFormatter = SceytChatUIKit.formatters.messageDateFormatter,
                messageBodyFormatter = SceytChatUIKit.formatters.channelLastMessageBodyFormatter,
                mentionUserNameFormatter = SceytChatUIKit.formatters.mentionUserNameFormatter,
                channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                attachmentIconProvider = SceytChatUIKit.providers.channelListAttachmentIconProvider,
                searchMessageResultBodyFormatter = SceytChatUIKit.formatters.searchMessageResultBodyFormatter,
                userAvatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer,
                channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer,
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
