package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FontRes
import androidx.annotation.LayoutRes
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme
import java.util.Date

/**
 * Style for [MessagesListViewStyle].
 * @property emptyState Layout resource for the empty state view, default is [R.layout.sceyt_messages_empty_state]
 * @property emptyStateForSelfChannel Layout resource for the empty state view for self channel, default is [R.layout.sceyt_messages_empty_state_self_channel]
 * @property loadingState Layout resource for the loading state view, default is [R.layout.sceyt_loading_state]
 * @property downScrollerUnreadCountColor Color for the unread count in the down scroller, default is [SceytChatUIKitTheme.accentColor]
 * @property downScrollerIcon Icon for the down scroller, default is [R.drawable.sceyt_scroll_next_button]
 * @property dateSeparatorTextFont Font for the date separator item text, default is -1
 * @property dateSeparatorTextStyle Style for the date separator item text, default is [Typeface.NORMAL]
 * @property dateSeparatorItemBackgroundColor Color for the date separator item background, default is [R.color.sceyt_color_overlay_background_2]
 * @property dateSeparatorItemTextColor Color for the date separator item text, default is [R.color.sceyt_color_on_primary]
 * @property unreadMessagesSeparatorTextStyle Style for the unread messages separator text, default is [Typeface.NORMAL]
 * @property unreadMessagesTextColor Color for the unread messages separator text, default is [SceytChatUIKitTheme.textSecondaryColor]
 * @property unreadMessagesBackendColor Background color for the unread messages separator, default is [SceytChatUIKitTheme.surface1Color]
 * @property sameSenderMsgDistance Distance between the same sender messages, default is 4dp
 * @property differentSenderMsgDistance Distance between the different sender messages, default is 8dp
 * @property enableScrollDownButton Enable scroll down button, default is true
 * @property enableDateSeparator Enable date separator, default is true
 * @property dateSeparatorDateFormat Date format for the date separator item, default is [SceytChatUIKitFormatters.messageDateSeparatorFormatter]
 * @property messageItemStyle Style for the message item view
 **/
data class MessagesListViewStyle(
        @ColorInt var backgroundColor: Int,
        @LayoutRes var emptyState: Int,
        @LayoutRes var emptyStateForSelfChannel: Int,
        @LayoutRes var loadingState: Int,
        @ColorInt var downScrollerUnreadCountColor: Int,
        var downScrollerIcon: Drawable?,
        @FontRes var dateSeparatorTextFont: Int = -1,
        var dateSeparatorTextStyle: Int = Typeface.NORMAL,
        @ColorInt var dateSeparatorItemBackgroundColor: Int,
        @ColorInt var dateSeparatorItemTextColor: Int,
        var unreadMessagesSeparatorTextStyle: Int = Typeface.NORMAL,
        @ColorInt var unreadMessagesTextColor: Int,
        @ColorInt var unreadMessagesBackendColor: Int,
        var enableScrollDownButton: Boolean,
        var enableDateSeparator: Boolean,
        @Dimension var sameSenderMsgDistance: Int = dpToPx(4f),
        @Dimension var differentSenderMsgDistance: Int = dpToPx(8f),
        var dateSeparatorDateFormat: Formatter<Date>,
        var messageItemStyle: MessageItemStyle
) {
    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessagesListViewStyle> { _, style -> style }

        internal var currentStyle: MessagesListViewStyle? = null
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessagesListViewStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListView).use { array ->
                val messageItemStyle = MessageItemStyle.Builder(context, attrs).build()

                val backgroundColor = array.getColor(R.styleable.MessagesListView_sceytUiMessageListBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

                val emptyState = array.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateLayout,
                    R.layout.sceyt_messages_empty_state)

                val emptyStateSelfChannel = array.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateSelfChannelLayout,
                    R.layout.sceyt_messages_empty_state_self_channel)

                val loadingState = array.getResourceId(R.styleable.MessagesListView_sceytUiLoadingStateLayout,
                    R.layout.sceyt_loading_state)

                val downScrollerUnreadCountColor = array.getColor(R.styleable.MessagesListView_sceytUiDownScrollerUnreadCountColor,
                    context.getCompatColor(SceytChatUIKit.theme.accentColor))

                val downScrollerIcon = array.getDrawable(R.styleable.MessagesListView_sceytUiDownScrollerIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_scroll_next_button)

                val dateSeparatorTextFont = array.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextFont, -1)

                val dateSeparatorTextStyle = array.getInt(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextStyle, Typeface.NORMAL)

                val dateSeparatorItemBackgroundColor = array.getColor(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.overlayBackgroundColor))

                val dateSeparatorItemTextColor = array.getColor(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor))

                val unreadMessagesSeparatorTextStyle = array.getInt(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorTextStyle, Typeface.NORMAL)

                val unreadMessagesBackgroundColor = array.getColor(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.surface1Color))

                val unreadMessagesTextColor = array.getColor(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorTextColor,
                    context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

                val sameSenderMsgDistance: Int = array.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiSameSenderMessageDistance,
                    dpToPx(4f))

                val differentSenderMsgDistance: Int = array.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiDifferentSenderMessageDistance,
                    dpToPx(8f))

                val enableScrollDownButton = array.getBoolean(R.styleable.MessagesListView_sceytUiEnableScrollDownButton, true)
                val enableDateSeparator = array.getBoolean(R.styleable.MessagesListView_sceytUiEnableDateSeparator, true)

                return MessagesListViewStyle(
                    backgroundColor = backgroundColor,
                    emptyState = emptyState,
                    emptyStateForSelfChannel = emptyStateSelfChannel,
                    loadingState = loadingState,
                    downScrollerUnreadCountColor = downScrollerUnreadCountColor,
                    downScrollerIcon = downScrollerIcon,
                    dateSeparatorTextFont = dateSeparatorTextFont,
                    dateSeparatorTextStyle = dateSeparatorTextStyle,
                    dateSeparatorItemBackgroundColor = dateSeparatorItemBackgroundColor,
                    dateSeparatorItemTextColor = dateSeparatorItemTextColor,
                    unreadMessagesSeparatorTextStyle = unreadMessagesSeparatorTextStyle,
                    unreadMessagesTextColor = unreadMessagesTextColor,
                    unreadMessagesBackendColor = unreadMessagesBackgroundColor,
                    sameSenderMsgDistance = sameSenderMsgDistance,
                    differentSenderMsgDistance = differentSenderMsgDistance,
                    enableScrollDownButton = enableScrollDownButton,
                    enableDateSeparator = enableDateSeparator,
                    dateSeparatorDateFormat = SceytChatUIKit.formatters.messageDateSeparatorFormatter,
                    messageItemStyle = messageItemStyle
                ).let { style ->
                    styleCustomizer.apply(context, style).also {
                        currentStyle = it
                    }
                }
            }
        }
    }
}