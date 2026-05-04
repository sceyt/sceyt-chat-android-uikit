package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.SearchMessageResultFormatterAttributes
import com.sceyt.chatuikit.renderers.ChannelAvatarRenderer
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMessageBodyTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildMetaTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSenderNameTextStyle
import java.util.Date

data class MediaSearchMessageItemStyle(
    @param:ColorInt val highlightTextColor: Int,
    val titleTextStyle: TextStyle,
    val messageBodyTextStyle: TextStyle,
    val dateTextStyle: TextStyle,
    val avatarStyle: AvatarStyle,
    val channelNameFormatter: Formatter<SceytChannel>,
    val messageDateFormatter: Formatter<Date>,
    val searchMessageResultBodyFormatter: Formatter<SearchMessageResultFormatterAttributes>,
    val channelAvatarRenderer: ChannelAvatarRenderer,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<MediaSearchMessageItemStyle> { _, style -> style }
    }

    internal class Builder(internal val context: Context) {

        fun build(): MediaSearchMessageItemStyle {
            val colors = SceytChatUIKit.theme.colors
            return MediaSearchMessageItemStyle(
                highlightTextColor = context.getCompatColor(colors.textPrimaryColor),
                titleTextStyle = buildSenderNameTextStyle(),
                messageBodyTextStyle = buildMessageBodyTextStyle(),
                dateTextStyle = buildMetaTextStyle(),
                avatarStyle = buildAvatarStyle(),
                channelNameFormatter = SceytChatUIKit.formatters.channelNameFormatter,
                messageDateFormatter = SceytChatUIKit.formatters.searchMessageDateFormatter,
                searchMessageResultBodyFormatter = SceytChatUIKit.formatters.searchMessageResultBodyFormatter,
                channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer,
            ).let { styleCustomizer.apply(context, it) }
        }

        private fun buildSenderNameTextStyle() =
            ChatsSearchMessageItemStyle.Builder(context).buildSenderNameTextStyle()

        private fun buildMessageBodyTextStyle() =
            ChatsSearchMessageItemStyle.Builder(context).buildMessageBodyTextStyle()

        private fun buildMetaTextStyle() =
            ChatsSearchMessageItemStyle.Builder(context).buildMetaTextStyle()

        private fun buildAvatarStyle() =
            ChatsSearchMessageItemStyle.Builder(context).buildAvatarStyle()
    }
}
