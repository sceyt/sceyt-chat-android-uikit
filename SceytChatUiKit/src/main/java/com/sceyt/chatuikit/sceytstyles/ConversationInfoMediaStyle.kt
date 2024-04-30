package com.sceyt.chatuikit.sceytstyles

import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.sceytconfigs.dateformaters.ConversationMediaDateFormatter

object ConversationInfoMediaStyle {
    @JvmField
    var mediaDateSeparatorFormat = ConversationMediaDateFormatter()

    @JvmField
    @DrawableRes
    var videoDurationIcon: Int = R.drawable.sceyt_ic_video
}
