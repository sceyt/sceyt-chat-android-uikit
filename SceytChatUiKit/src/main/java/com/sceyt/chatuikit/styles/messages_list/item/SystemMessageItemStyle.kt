package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.content.res.TypedArray
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildTextStyle

data class SystemMessageItemStyle(
    val textStyle: TextStyle,
    val textFormatter: Formatter<SceytMessage>,
    val backgroundStyle: BackgroundStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<SystemMessageItemStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        internal val typedArray: TypedArray
    ) {
        fun build() = SystemMessageItemStyle(
            textStyle = buildTextStyle(),
            textFormatter = SceytChatUIKit.formatters.systemMessageBodyFormatter,
            backgroundStyle = buildBackgroundStyle()
        ).let { styleCustomizer.apply(context, it) }
    }
}