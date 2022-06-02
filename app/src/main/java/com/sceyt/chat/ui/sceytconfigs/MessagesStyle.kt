package com.sceyt.chat.ui.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import com.sceyt.chat.ui.R

object MessagesStyle {
    const val INC_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val INC_EDITED_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val OUT_DEFAULT_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"
    const val OUT_EDITED_SPACE = "&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;"

    @ColorRes
    var incBubbleColor: Int = R.color.sceyt_color_bg_inc_message
    var outBubbleColor: Int = R.color.sceyt_color_bg_out_message

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        return this
    }
}