package com.sceyt.sceytchatuikit.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import com.sceyt.sceytchatuikit.R

object MessagesStyle {
    const val INC_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val INC_EDITED_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val OUT_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val OUT_EDITED_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"

    @ColorRes
    var incBubbleColor: Int = R.color.sceyt_color_bg_inc_message

    @ColorRes
    var outBubbleColor: Int = R.color.sceyt_color_bg_out_message

    @LayoutRes
    var emptyState: Int = R.layout.sceyt_messages_empty_state

    @LayoutRes
    var loadingState: Int = R.layout.sceyt_loading_state

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        return this
    }
}