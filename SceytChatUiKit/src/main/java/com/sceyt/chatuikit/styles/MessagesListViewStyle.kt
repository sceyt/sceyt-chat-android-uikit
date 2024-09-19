package com.sceyt.chatuikit.styles

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.components.channel.messages.MessagesListView
import com.sceyt.chatuikit.formatters.MessageDateSeparatorFormatter
import com.sceyt.chatuikit.formatters.SceytChatUIKitFormatters
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [MessagesListView] component.
 * @param emptyState Layout resource for the empty state view, default is [R.layout.sceyt_messages_empty_state]
 * @param emptyStateSelfChannel Layout resource for the empty state view for self channel, default is [R.layout.sceyt_messages_empty_state_self_channel]
 * @param loadingState Layout resource for the loading state view, default is [R.layout.sceyt_loading_state]
 * @param downScrollerUnreadCountColor Color for the unread count in the down scroller, default is [SceytChatUIKitTheme.accentColor]
 * @param downScrollerIcon Icon for the down scroller, default is [R.drawable.sceyt_scroll_down_button]
 * @param dateSeparatorTextFont Font for the date separator item text, default is -1
 * @param dateSeparatorTextStyle Style for the date separator item text, default is [Typeface.NORMAL]
 * @param dateSeparatorItemBackgroundColor Color for the date separator item background, default is [R.color.sceyt_color_overlay_background_2]
 * @param dateSeparatorItemTextColor Color for the date separator item text, default is [R.color.sceyt_color_on_primary]
 * @param unreadMessagesSeparatorTextStyle Style for the unread messages separator text, default is [Typeface.NORMAL]
 * @param unreadMessagesTextColor Color for the unread messages separator text, default is [SceytChatUIKitTheme.textSecondaryColor]
 * @param unreadMessagesBackendColor Background color for the unread messages separator, default is [SceytChatUIKitTheme.surface1Color]
 * @param messageItemStyle Style for the message item view
 * @param enableScrollDownButton Enable scroll down button, default is true
 * @param enableDateSeparator Enable date separator, default is true
 * @param dateSeparatorDateFormat Date format for the date separator item, default is [SceytChatUIKitFormatters.messageDateSeparatorFormatter]
 **/
data class MessagesListViewStyle(
        @ColorInt val backgroundColor: Int,
        @LayoutRes val emptyState: Int,
        @LayoutRes val emptyStateSelfChannel: Int,
        @LayoutRes val loadingState: Int,
        @ColorInt val downScrollerUnreadCountColor: Int,
        val downScrollerIcon: Drawable?,
        @FontRes val dateSeparatorTextFont: Int = -1,
        val dateSeparatorTextStyle: Int = Typeface.NORMAL,
        @ColorInt val dateSeparatorItemBackgroundColor: Int,
        @ColorInt val dateSeparatorItemTextColor: Int,
        val unreadMessagesSeparatorTextStyle: Int = Typeface.NORMAL,
        @ColorInt val unreadMessagesTextColor: Int,
        @ColorInt val unreadMessagesBackendColor: Int,
        val messageItemStyle: MessageItemStyle,
        val enableScrollDownButton: Boolean,
        val enableDateSeparator: Boolean,
        val dateSeparatorDateFormat: MessageDateSeparatorFormatter,
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
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MessagesListView)

            val messageItemStyle = MessageItemStyle.Builder(context, attrs).build()

            val backgroundColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiMessageListBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.backgroundColor))

            val emptyState = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateLayout,
                R.layout.sceyt_messages_empty_state)

            val emptyStateSelfChannel = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiEmptyStateSelfChannelLayout,
                R.layout.sceyt_messages_empty_state_self_channel)

            val loadingState = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiLoadingStateLayout,
                R.layout.sceyt_loading_state)

            val downScrollerUnreadCountColor = typedArray.getColor(R.styleable.MessagesListView_sceytUiDownScrollerUnreadCountColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val downScrollerIcon = typedArray.getDrawable(R.styleable.MessagesListView_sceytUiDownScrollerIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_scroll_down_button)

            val dateSeparatorTextFont: Int = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextFont, -1)

            val dateSeparatorTextStyle: Int = typedArray.getInt(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextStyle, Typeface.NORMAL)

            val dateSeparatorItemBackgroundColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.overlayBackgroundColor))

            val dateSeparatorItemTextColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor,
                context.getCompatColor(SceytChatUIKit.theme.onPrimaryColor))

            val unreadMessagesSeparatorTextStyle: Int = typedArray.getInt(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorTextStyle, Typeface.NORMAL)

            val unreadMessagesBackgroundColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorBackgroundColor,
                context.getCompatColor(SceytChatUIKit.theme.surface1Color))

            val unreadMessagesTextColor: Int = typedArray.getColor(R.styleable.MessagesListView_sceytUiUnreadMessagesSeparatorTextColor,
                context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))

            val enableScrollDownButton = typedArray.getBoolean(R.styleable.MessagesListView_sceytUiEnableScrollDownButton, true)
            val enableDateSeparator = typedArray.getBoolean(R.styleable.MessagesListView_sceytUiEnableDateSeparator, true)

            val dateSeparatorDateFormat = SceytChatUIKit.formatters.messageDateSeparatorFormatter
            typedArray.recycle()

            return MessagesListViewStyle(
                backgroundColor = backgroundColor,
                emptyState = emptyState,
                emptyStateSelfChannel = emptyStateSelfChannel,
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
                messageItemStyle = messageItemStyle,
                enableScrollDownButton = enableScrollDownButton,
                enableDateSeparator = enableDateSeparator,
                dateSeparatorDateFormat = dateSeparatorDateFormat
            ).let { style ->
                styleCustomizer.apply(context, style).also {
                    currentStyle = it
                }
            }
        }
    }
}