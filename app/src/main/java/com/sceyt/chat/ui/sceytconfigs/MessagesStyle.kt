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
    var incBubbleColor: Int = R.color.colorGrayThemed
    var outBubbleColor: Int = R.color.colorBgOutMessage

    internal fun updateWithAttributes(typedArray: TypedArray): MessagesStyle {
        incBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageIncBubbleColor, incBubbleColor)
        outBubbleColor = typedArray.getResourceId(R.styleable.MessagesListView_sceytUiMessageOutBubbleColor, outBubbleColor)
        return this
    }
}