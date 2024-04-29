package com.sceyt.chatuikit.sceytstyles

import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.sceytconfigs.dateformaters.ConversationMediaDateFormatter

object ConversationInfoMediaStyle {
    @JvmField
    var mediaDateSeparatorFormat = ConversationMediaDateFormatter()

    @JvmField
    var spaceBetweenSections = dpToPx(16f)
}
