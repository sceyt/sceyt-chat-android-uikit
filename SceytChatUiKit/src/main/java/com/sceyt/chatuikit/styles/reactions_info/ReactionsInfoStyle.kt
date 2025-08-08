package com.sceyt.chatuikit.styles.reactions_info

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.ReactionsInfoBottomSheetFragment
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.extensions.reaction_info.buildBackgroundStyle
import com.sceyt.chatuikit.theme.Colors
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for [ReactionsInfoBottomSheetFragment].
 * @property dividerColor Color of the divider between the header and the list of users, default is [Colors.borderColor].
 * @property backgroundStyle Style for the background of the view, default is [buildBackgroundStyle].
 * @property headerItemStyle Style for the header item.
 * @property reactedUserListStyle Style for the list of users who reacted to a message.
 * */
data class ReactionsInfoStyle(
        @param:ColorInt val dividerColor: Int,
        val backgroundStyle: BackgroundStyle,
        val headerItemStyle: ReactionsInfoHeaderItemStyle,
        val reactedUserListStyle: ReactedUserListStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<ReactionsInfoStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): ReactionsInfoStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.ReactionInfo).use { array ->
                val dividerColor = context.getCompatColor(SceytChatUIKitTheme.colors.borderColor)

                return ReactionsInfoStyle(
                    dividerColor = dividerColor,
                    backgroundStyle = buildBackgroundStyle(array),
                    headerItemStyle = ReactionsInfoHeaderItemStyle.Builder(context, attributeSet).build(),
                    reactedUserListStyle = ReactedUserListStyle.Builder(context, attributeSet).build()
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}
