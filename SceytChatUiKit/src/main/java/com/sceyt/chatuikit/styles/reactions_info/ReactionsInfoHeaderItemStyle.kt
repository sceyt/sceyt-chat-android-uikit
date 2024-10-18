package com.sceyt.chatuikit.styles.reactions_info

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.reaction_info.buildSelectedTextStyle
import com.sceyt.chatuikit.styles.extensions.reaction_info.buildTextStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for the header item in the reactions info view.
 * @property backgroundColor Background color of the item, default is [Color.TRANSPARENT].
 * @property selectedBackgroundColor Background color of the item when it is selected, default is [Colors.accentColor].
 * @property borderColor Border color of the item, default is [Colors.borderColor].
 * @property selectedBorderColor Border color of the item when it is selected, default is [Color.TRANSPARENT].
 * @property borderWidth Border width of the item, default is 1dp.
 * @property cornerRadius Corner radius of the item, default is 30dp.
 * @property textStyle Style for the text in the item, default is [TextStyle].
 * @property selectedTextStyle Style for the text in the item when it is selected, default is [TextStyle].
 * */
data class ReactionsInfoHeaderItemStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val selectedBackgroundColor: Int,
        @ColorInt val borderColor: Int,
        @ColorInt val selectedBorderColor: Int,
        val borderWidth: Int,
        val cornerRadius: Int,
        val textStyle: TextStyle,
        val selectedTextStyle: TextStyle
) {
    companion object {
        var styleCustomizer = StyleCustomizer<ReactionsInfoHeaderItemStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ReactionsInfoHeaderItemStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.ReactionInfo).use { array ->
                val backgroundColor = Color.TRANSPARENT
                val selectedBackgroundColor = context.getCompatColor(SceytChatUIKitTheme.colors.accentColor)
                val borderColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor)
                val borderWidth = 1.dpToPx()
                val cornerRadius = 30.dpToPx()

                return ReactionsInfoHeaderItemStyle(
                    backgroundColor = backgroundColor,
                    selectedBackgroundColor = selectedBackgroundColor,
                    borderColor = borderColor,
                    selectedBorderColor = 0,
                    borderWidth = borderWidth,
                    cornerRadius = cornerRadius,
                    textStyle = buildTextStyle(array),
                    selectedTextStyle = buildSelectedTextStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
