package com.sceyt.sceytchatuikit.sceytstyles

import androidx.annotation.ColorRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.dpToPx
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.ConversationMediaDateFormatter

object ConversationInfoMediaStyle {
    @JvmField
    var mediaDateSeparatorFormat = ConversationMediaDateFormatter()

    @JvmField
    var dividerHeight = dpToPx(16f)

    @JvmField
    @ColorRes
    var dividerColor: Int = R.color.sceyt_color_divider
}
