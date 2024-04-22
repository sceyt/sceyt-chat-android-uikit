package com.sceyt.chatuikit.sceytstyles

import androidx.annotation.ColorRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.sceytconfigs.dateformaters.ConversationMediaDateFormatter

object ConversationInfoMediaStyle {
    @JvmField
    var mediaDateSeparatorFormat = ConversationMediaDateFormatter()

    @JvmField
    var dividerHeight = dpToPx(16f)

    @JvmField
    @ColorRes
    var dividerColor: Int = SceytChatUIKit.theme.backgroundColorSecondary
}
