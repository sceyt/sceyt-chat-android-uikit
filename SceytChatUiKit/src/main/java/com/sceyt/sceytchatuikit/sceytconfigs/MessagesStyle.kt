package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.res.TypedArray
import android.graphics.Typeface
import androidx.annotation.*
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.MessageDateSeparatorFormatter

object MessagesStyle {
    const val INC_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val INC_EDITED_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val OUT_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val OUT_EDITED_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"

    @ColorRes
    var incBubbleColor: Int = R.color.sceyt_color_bg_inc_message

    @ColorRes
    var outBubbleColor: Int = R.color.sceyt_color_bg_out_message

    @ColorRes
    var senderNameTextColor: Int = R.color.sceyt_color_accent

    @ColorRes
    var replayMessageLineColor: Int = R.color.sceyt_color_accent

    @DrawableRes
    var dateSeparatorItemBackground = R.drawable.sceyt_bg_message_day

    @ColorRes
    var dateSeparatorItemTextColor = R.color.sceyt_color_gray_400

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_messages_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_loading_state

    var dateSeparatorDateFormat = MessageDateSeparatorFormatter()

    @FontRes
    var dateSeparatorTextFont: Int = -1

    var dateSeparatorTextStyle: Int = Typeface.NORMAL

    @Dimension
    var sameSenderMsgDistance = dpToPx(4f)

    @Dimension
    var differentSenderMsgDistance = dpToPx(8f)

    @ColorRes
    var downScrollerUnreadCountColor: Int = R.color.sceyt_color_accent

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        senderNameTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageSenderNameTextColor, senderNameTextColor)
        replayMessageLineColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiReplayMessageLineColor, replayMessageLineColor)
        dateSeparatorItemBackground = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackground, dateSeparatorItemBackground)
        dateSeparatorItemTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor, dateSeparatorItemTextColor)
        sameSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiSameMessageSenderDistance, sameSenderMsgDistance)
        differentSenderMsgDistance = typedArray.getDimensionPixelSize(R.styleable.MessagesListView_sceytUiDifferentMessageSenderDistance, differentSenderMsgDistance)
        dateSeparatorTextFont = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextFont, dateSeparatorTextFont)
        dateSeparatorTextStyle = typedArray.getInt(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextStyle, dateSeparatorTextStyle)
        downScrollerUnreadCountColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDownScrollerUnreadCountColor, downScrollerUnreadCountColor)
        return this
    }
}