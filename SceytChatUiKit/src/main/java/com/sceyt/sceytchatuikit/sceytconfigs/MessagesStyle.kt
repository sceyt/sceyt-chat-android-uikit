package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.sceyt.sceytchatuikit.R
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

    @DrawableRes
    var dateSeparatorItemBackground = R.drawable.sceyt_bg_message_day

    @ColorRes
    var dateSeparatorItemTextColor = R.color.sceyt_color_gray_400

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_messages_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_loading_state

    var dateSeparatorDateFormat = MessageDateSeparatorFormatter()

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        dateSeparatorItemBackground = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemBackground, dateSeparatorItemBackground)
        dateSeparatorItemTextColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiDateSeparatorItemTextColor, dateSeparatorItemTextColor)
        return this
    }
}