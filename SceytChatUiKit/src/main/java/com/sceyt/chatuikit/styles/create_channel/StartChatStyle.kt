package com.sceyt.chatuikit.styles.create_channel

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.startchat.StartChatActivity
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.start_chat.buildCreateChannelTextStyle
import com.sceyt.chatuikit.styles.extensions.start_chat.buildCreateGroupTextStyle
import com.sceyt.chatuikit.styles.extensions.start_chat.buildItemStyle
import com.sceyt.chatuikit.styles.extensions.start_chat.buildSearchToolbarStyle
import com.sceyt.chatuikit.styles.extensions.start_chat.buildSeparatorTextStyle
import com.sceyt.chatuikit.theme.Colors

typealias StartChatUsersItemStyle = ListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, AvatarRenderer<SceytUser>>

/**
 * Style for the [StartChatActivity].
 * @param backgroundColor Background color of the screen. Default is [Colors.backgroundColor].
 * @param createChannelIcon Icon for creating a channel. Default is [R.drawable.sceyt_ic_create_channel].
 * @param createGroupIcon Icon for creating a group. Default is [R.drawable.sceyt_ic_create_group].
 * @param toolbarTitle Title for the toolbar. Default is [R.string.sceyt_start_chat].
 * @param createGroupText Text for creating a group. Default is [R.string.sceyt_new_group].
 * @param createChannelText Text for creating a channel. Default is [R.string.sceyt_new_channel].
 * @param separatorText Text for the separator. Default is [R.string.sceyt_users].
 * @param createGroupTextStyle Style for the create group text. Default is [buildCreateGroupTextStyle].
 * @param createChannelTextStyle Style for the create channel text. Default is [buildCreateChannelTextStyle].
 * @param separatorTextStyle Style for the separator text. Default is [buildSeparatorTextStyle].
 * @param toolbarStyle Style for the toolbar. Default is [buildSearchToolbarStyle].
 * @param itemStyle Style for the items in the list. Default is [buildItemStyle].
 * */
data class StartChatStyle(
        @ColorInt val backgroundColor: Int,
        val createChannelIcon: Drawable?,
        val createGroupIcon: Drawable?,
        val toolbarTitle: String,
        val createGroupText: String,
        val createChannelText: String,
        val separatorText: String,
        val createGroupTextStyle: TextStyle,
        val createChannelTextStyle: TextStyle,
        val separatorTextStyle: TextStyle,
        val toolbarStyle: SearchToolbarStyle,
        val itemStyle: StartChatUsersItemStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<StartChatStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attributeSet: AttributeSet?,
    ) {
        fun build(): StartChatStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.StartChat).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                val createGroupIcon = context.getCompatDrawable(R.drawable.sceyt_ic_create_group).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )
                val createChannelIcon = context.getCompatDrawable(R.drawable.sceyt_ic_create_channel).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )
                val toolbarTitle = context.getString(R.string.sceyt_start_chat)
                val createGroupText = context.getString(R.string.sceyt_new_group)
                val createChannelText = context.getString(R.string.sceyt_new_channel)
                val separatorText = context.getString(R.string.sceyt_users)

                return StartChatStyle(
                    backgroundColor = backgroundColor,
                    createChannelIcon = createChannelIcon,
                    createGroupIcon = createGroupIcon,
                    toolbarTitle = toolbarTitle,
                    createGroupText = createGroupText,
                    createChannelText = createChannelText,
                    separatorText = separatorText,
                    createGroupTextStyle = buildCreateGroupTextStyle(array),
                    createChannelTextStyle = buildCreateChannelTextStyle(array),
                    separatorTextStyle = buildSeparatorTextStyle(array),
                    toolbarStyle = buildSearchToolbarStyle(array),
                    itemStyle = buildItemStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}