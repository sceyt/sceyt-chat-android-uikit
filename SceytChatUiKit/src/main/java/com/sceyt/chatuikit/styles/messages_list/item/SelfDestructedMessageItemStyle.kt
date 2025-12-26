package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.extensions.messages_list.buildSelfDestructedMessageIconColor

data class SelfDestructedMessageItemStyle(
    val drawable: Drawable?,
    @param:ColorInt val iconColor: Int,
    val bodyFormatter: Formatter<SceytMessage>,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<SelfDestructedMessageItemStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        internal val typedArray: TypedArray
    ) {
        fun build() = SelfDestructedMessageItemStyle(
            drawable = context.getCompatDrawable(R.drawable.sceyt_ic_message_self_destructed),
            iconColor = buildSelfDestructedMessageIconColor(),
            bodyFormatter = SceytChatUIKit.formatters.selfDestructedMessageBodyFormatter
        ).let { styleCustomizer.apply(context, it) }
    }
}