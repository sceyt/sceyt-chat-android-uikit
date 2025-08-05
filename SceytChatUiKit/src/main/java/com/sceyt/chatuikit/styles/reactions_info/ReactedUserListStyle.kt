package com.sceyt.chatuikit.styles.reactions_info

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.extensions.reaction_info.buildUserItemStyle

typealias ReactedUserItemStyle = ListItemStyle<Formatter<SceytUser>, Formatter<String>, AvatarRenderer<SceytUser>>

/**
 *Style for the list of users who reacted to a message.
 * @property backgroundColor Background color of the list, default is [Color.TRANSPARENT].
 * @property itemStyle Style for the user item in the list, default is [buildUserItemStyle].
 * */
data class ReactedUserListStyle(
        @ColorInt val backgroundColor: Int,
        val itemStyle: ReactedUserItemStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ReactedUserListStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): ReactedUserListStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.ReactionInfo).use { array ->
                return ReactedUserListStyle(
                    backgroundColor = Color.TRANSPARENT,
                    itemStyle = buildUserItemStyle(array),
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
