package com.sceyt.chatuikit.styles.create_channel

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.UserFormatter
import com.sceyt.chatuikit.presentation.components.create_chat.create_group.CreateGroupActivity
import com.sceyt.chatuikit.renderers.UserAvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.create_group.buildAboutTextFieldStyle
import com.sceyt.chatuikit.styles.extensions.create_group.buildActionButtonStyle
import com.sceyt.chatuikit.styles.extensions.create_group.buildNameTextFieldStyle
import com.sceyt.chatuikit.styles.extensions.create_group.buildSeparatorTextStyle
import com.sceyt.chatuikit.styles.extensions.create_group.buildToolbarStyle
import com.sceyt.chatuikit.styles.extensions.create_group.buildUserItemStyle
import com.sceyt.chatuikit.theme.Colors

typealias CreateGroupStyleUserItemStyle = ListItemStyle<UserFormatter, UserFormatter, UserAvatarRenderer>

/**
 * Style for the [CreateGroupActivity] page.
 * @param backgroundColor Background color of the page. Default is [Colors.backgroundColor].
 * @param avatarBackgroundColor Background color of the avatar. Default is [Colors.overlayBackground2Color].
 * @param dividerColor Color of the dividers. Default is [Colors.borderColor].
 * @param avatarDefaultIcon Default icon for the avatar. Default is [R.drawable.sceyt_ic_camera_72].
 * @param separatorText Text for the separator. Default is [R.string.sceyt_members].
 * @param separatorTextStyle Style for the separator text. Default is [buildSeparatorTextStyle].
 * @param toolbarStyle Style for the toolbar. Default is [buildToolbarStyle].
 * @param nameTextFieldStyle Style for the name text field. Default is [buildNameTextFieldStyle].
 * @param aboutTextFieldStyle Style for the about text field. Default is [buildAboutTextFieldStyle].
 * @param actionButtonStyle Style for the action button. Default is [buildActionButtonStyle].
 * @param userItemStyle Style for the user item. Default is [buildUserItemStyle].
 * */
data class CreateGroupStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val avatarBackgroundColor: Int,
        @param:ColorInt val dividerColor: Int,
        val avatarDefaultIcon: Drawable?,
        val separatorText: String,
        val separatorTextStyle: TextStyle,
        val toolbarStyle: ToolbarStyle,
        val nameTextFieldStyle: TextInputStyle,
        val aboutTextFieldStyle: TextInputStyle,
        val actionButtonStyle: ButtonStyle,
        val userItemStyle: CreateGroupStyleUserItemStyle,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<CreateGroupStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): CreateGroupStyle {
            context.obtainStyledAttributes(attrs, R.styleable.CreateGroup).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                val avatarBackgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackground2Color)
                val dividerColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
                val avatarDefaultIcon = context.getCompatDrawable(R.drawable.sceyt_ic_camera_72).applyTint(
                    context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                )
                val separatorText = context.getString(R.string.sceyt_members)

                return CreateGroupStyle(
                    backgroundColor = backgroundColor,
                    avatarBackgroundColor = avatarBackgroundColor,
                    dividerColor = dividerColor,
                    avatarDefaultIcon = avatarDefaultIcon,
                    separatorText = separatorText,
                    separatorTextStyle = buildSeparatorTextStyle(array),
                    toolbarStyle = buildToolbarStyle(array),
                    nameTextFieldStyle = buildNameTextFieldStyle(array),
                    aboutTextFieldStyle = buildAboutTextFieldStyle(array),
                    actionButtonStyle = buildActionButtonStyle(array),
                    userItemStyle = buildUserItemStyle(array),
                )
            }
        }
    }
}
