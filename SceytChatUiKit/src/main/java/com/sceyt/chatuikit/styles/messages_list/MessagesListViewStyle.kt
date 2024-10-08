package com.sceyt.chatuikit.styles.messages_list

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.LayoutRes
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.extensions.messages_list.buildDateSeparatorStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildReactionPickerStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildScrollDownButtonStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildUnreadMessagesSeparatorStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

/**
 * Style for [MessagesListViewStyle].
 * @property backgroundColor Background color of the message list view
 * @property emptyState Layout resource for the empty state view,default is [R.layout.sceyt_messages_empty_state]
 * @property emptyStateForSelfChannel Layout resource for the empty state view for self channel,default is [R.layout.sceyt_messages_empty_state_self_channel]
 * @property loadingState Layout resource for the loading state view,default is [R.layout.sceyt_loading_state]
 * @property sameSenderMessageDistance Distance between two messages from the same sender, default is 4dp
 * @property differentSenderMessageDistance Distance between two messages from different senders, default is 8dp
 * @property scrollDownButtonStyle Style for the scroll down button, default is [buildScrollDownButtonStyle]
 * @property dateSeparatorStyle Style for the date separator, default is [buildDateSeparatorStyle]
 * @property unreadMessagesSeparatorStyle Style for the unread messages separator, default is [buildUnreadMessagesSeparatorStyle]
 * @property reactionPickerStyle Style for the reaction picker, default is [buildReactionPickerStyle]
 * @property enableScrollDownButton Enable scroll down button, default is true
 * @property enableDateSeparator Enable date separator, default is true
 * @property messageItemStyle Style for the message item view
 **/
data class MessagesListViewStyle(
        @ColorInt val backgroundColor: Int,
        @LayoutRes val emptyState: Int,
        @LayoutRes val emptyStateForSelfChannel: Int,
        @LayoutRes val loadingState: Int,
        @Dimension val sameSenderMessageDistance: Int,
        @Dimension val differentSenderMessageDistance: Int,
        val messageItemStyle: MessageItemStyle,
        val scrollDownButtonStyle: ScrollDownButtonStyle,
        val dateSeparatorStyle: DateSeparatorStyle,
        val unreadMessagesSeparatorStyle: UnreadMessagesSeparatorStyle,
        val reactionPickerStyle: ReactionPickerStyle,
        val enableScrollDownButton: Boolean,
        val enableDateSeparator: Boolean,
) {
    companion object {
        @JvmField
        var styleCustomizer = StyleCustomizer<MessagesListViewStyle> { _, style -> style }

        internal var currentStyle: MessagesListViewStyle? = null
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): MessagesListViewStyle {
            context.obtainStyledAttributes(attrs, R.styleable.MessagesListView).use { array ->
                val backgroundColor = array.getColor(R.styleable.MessagesListView_sceytUiMessagesListBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))

                val emptyState = array.getResourceId(R.styleable.MessagesListView_sceytUiMessagesListEmptyStateLayout,
                    R.layout.sceyt_messages_empty_state)

                val emptyStateSelfChannel = array.getResourceId(R.styleable.MessagesListView_sceytUiMessagesListEmptyStateSelfChannelLayout,
                    R.layout.sceyt_messages_empty_state_self_channel)

                val loadingState = array.getResourceId(R.styleable.MessagesListView_sceytUiMessagesListLoadingStateLayout,
                    R.layout.sceyt_loading_state)

                val sameSenderMessageDistance = array.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiMessagesListSameSenderMessageDistance,
                    dpToPx(4f))

                val differentSenderMessageDistance = array.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiMessagesListDifferentSenderMessageDistance,
                    dpToPx(8f))

                val messageItemStyle = MessageItemStyle.Builder(context, attrs).build()

                val enableScrollDownButton = array.getBoolean(R.styleable.MessagesListView_sceytUiEnableScrollDownButton, true)
                val enableDateSeparator = array.getBoolean(R.styleable.MessagesListView_sceytUiEnableDateSeparator, true)

                return MessagesListViewStyle(
                    backgroundColor = backgroundColor,
                    emptyState = emptyState,
                    emptyStateForSelfChannel = emptyStateSelfChannel,
                    loadingState = loadingState,
                    sameSenderMessageDistance = sameSenderMessageDistance,
                    differentSenderMessageDistance = differentSenderMessageDistance,
                    messageItemStyle = messageItemStyle,
                    scrollDownButtonStyle = buildScrollDownButtonStyle(array),
                    dateSeparatorStyle = buildDateSeparatorStyle(array),
                    unreadMessagesSeparatorStyle = buildUnreadMessagesSeparatorStyle(array),
                    reactionPickerStyle = buildReactionPickerStyle(array),
                    enableScrollDownButton = enableScrollDownButton,
                    enableDateSeparator = enableDateSeparator
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}