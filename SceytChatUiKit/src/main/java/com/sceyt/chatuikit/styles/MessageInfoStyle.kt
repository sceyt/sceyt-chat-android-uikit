package com.sceyt.chatuikit.styles

import android.content.Context
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.MarkerType
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.presentation.components.message_info.MessageInfoFragment
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.SceytChatUIKitProviders
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.theme.Colors
import java.util.Date

/**
 * Style for [MessageInfoFragment]
 * @property backgroundColor Background color of the fragment, default is [Colors.backgroundColor]
 * @property toolbarColor Color of the toolbar, default is [Colors.primaryColor]
 * @property borderColor Color of the border, default is [Colors.borderColor]
 * @property toolbarTitle Title of the fragment, default is [R.string.sceyt_message_info]
 * @property sentLabelText Label for the sent text, default is [R.string.sceyt_sent]
 * @property sizeLabelText Label for the size text, default is [R.string.sceyt_size]
 * @property descriptionTitleTextStyle Style for the title of the description
 * @property descriptionValueTextStyle Style for the value of the description
 * @property headerTextStyle Style for the header text
 * @property toolbarStyle Style for the toolbar
 * @property messageItemStyle Style for the message items, default is [MessagesListViewStyle.currentStyle]
 * @property listItemStyle Style for the list items
 * @property messageDateFormatter Formatter for the message date, default is [SceytChatUIKitFormatters.messageInfoDateFormatter]
 * @property attachmentSizeFormatter Formatter for the attachment size, default is [SceytChatUIKitFormatters.attachmentSizeFormatter]
 * @property markerTitleProvider Provider for the marker title, default is [SceytChatUIKitProviders.markerTitleProvider]
 * */
data class MessageInfoStyle(
        @ColorInt var backgroundColor: Int,
        @ColorInt var toolbarColor: Int,
        @ColorInt var borderColor: Int,
        var toolbarTitle: String,
        var sentLabelText: String,
        var sizeLabelText: String,
        var descriptionTitleTextStyle: TextStyle,
        var descriptionValueTextStyle: TextStyle,
        var headerTextStyle: TextStyle,
        var toolbarStyle: ToolbarStyle,
        var messageItemStyle: MessageItemStyle,
        var listItemStyle: ListItemStyle<Formatter<SceytUser>, Formatter<Date>, VisualProvider<SceytUser, DefaultAvatar>>,
        var messageDateFormatter: Formatter<Date>,
        var attachmentSizeFormatter: Formatter<SceytAttachment>,
        var markerTitleProvider: VisualProvider<MarkerType, String>
) {

    companion object {
        var styleCustomizer = StyleCustomizer<MessageInfoStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context
    ) {
        fun build(): MessageInfoStyle {

            val descriptionTitleTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                font = R.font.roboto_medium
            )

            val descriptionValueTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
            )

            val headerTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
                font = R.font.roboto_medium
            )

            val toolbarStyle = ToolbarStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
                borderColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
                navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back),
                titleTextStyle = TextStyle(
                    color = context.getCompatColor(R.color.sceyt_color_text_primary),
                    font = R.font.roboto_medium
                )
            )

            val userNameTextStyle = TextStyle(
                color = context.getCompatColor(R.color.sceyt_color_text_primary),
                font = R.font.roboto_regular
            )

            val dateTextStyle = TextStyle(
                color = context.getCompatColor(R.color.sceyt_color_text_primary),
                font = R.font.roboto_regular
            )

            val listItemStyle = ListItemStyle(
                titleTextStyle = userNameTextStyle,
                subtitleTextStyle = dateTextStyle,
                titleFormatter = SceytChatUIKit.formatters.userNameFormatterNew,
                subtitleFormatter = SceytChatUIKit.formatters.messageDateFormatter,
                avatarProvider = SceytChatUIKit.providers.userDefaultAvatarProvider
            )

            return MessageInfoStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
                toolbarColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
                borderColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
                toolbarTitle = context.getString(R.string.sceyt_message_info),
                sentLabelText = context.getString(R.string.sceyt_sent),
                sizeLabelText = context.getString(R.string.sceyt_size),
                descriptionTitleTextStyle = descriptionTitleTextStyle,
                descriptionValueTextStyle = descriptionValueTextStyle,
                headerTextStyle = headerTextStyle,
                toolbarStyle = toolbarStyle,
                listItemStyle = listItemStyle,
                messageItemStyle = MessagesListViewStyle.currentStyle?.messageItemStyle
                        ?: MessageItemStyle.Builder(context, null).build(),
                messageDateFormatter = SceytChatUIKit.formatters.messageInfoDateFormatter,
                attachmentSizeFormatter = SceytChatUIKit.formatters.attachmentSizeFormatter,
                markerTitleProvider = SceytChatUIKit.providers.markerTitleProvider
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}