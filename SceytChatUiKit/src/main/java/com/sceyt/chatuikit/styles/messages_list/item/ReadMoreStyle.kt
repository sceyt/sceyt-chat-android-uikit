package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.content.res.TypedArray
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.messages_list.buildText
import com.sceyt.chatuikit.styles.extensions.messages_list.buildTextStyle

/**
 * Style for the "Read More" button in expandable message text.
 * @property text Text to display for the read more button, default is "Read More"
 * @property textStyle Style for the read more text (color, size, font, style)
 */
data class ReadMoreStyle(
    val text: String,
    val textStyle: TextStyle
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ReadMoreStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        internal val typedArray: TypedArray
    ) {
        fun build() = ReadMoreStyle(
            text = buildText(),
            textStyle = buildTextStyle()
        ).let { styleCustomizer.apply(context, it) }
    }
}