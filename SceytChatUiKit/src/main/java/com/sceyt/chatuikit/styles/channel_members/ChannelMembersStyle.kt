package com.sceyt.chatuikit.styles.channel_members

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

data class ChannelMembersStyle(
        @ColorInt var backgroundColor: Int,
        var addMembersIcon: Drawable?,
        var addMemberTextStyle: TextStyle,
        var toolbarStyle: ToolbarStyle,
        var itemStyle: ChannelMemberListItemStyle,
) {
    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): ChannelMembersStyle {
            val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
            val addMembersIcon = context.getCompatDrawable(R.drawable.sceyt_ic_add_members).applyTint(
                context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            )
            val addMemberTextStyle = TextStyle(
                color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
            )
            val toolbarStyle = ToolbarStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
                borderColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
                navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                ),
                titleTextStyle = TextStyle(
                    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
                    font = R.font.roboto_medium
                )
            )

            val itemStyle = ChannelMemberListItemStyle.Builder(context, attributeSet).build()

            return ChannelMembersStyle(
                backgroundColor = backgroundColor,
                addMembersIcon = addMembersIcon,
                addMemberTextStyle = addMemberTextStyle,
                toolbarStyle = toolbarStyle,
                itemStyle = itemStyle
            )
        }
    }
}