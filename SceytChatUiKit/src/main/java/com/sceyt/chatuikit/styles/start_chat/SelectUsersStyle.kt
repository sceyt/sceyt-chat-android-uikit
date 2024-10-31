package com.sceyt.chatuikit.styles.start_chat

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle
import com.sceyt.chatuikit.styles.common.SelectedListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.select_users.buildActionButtonStyle
import com.sceyt.chatuikit.styles.extensions.select_users.buildItemStyle
import com.sceyt.chatuikit.styles.extensions.select_users.buildSearchToolbarStyle
import com.sceyt.chatuikit.styles.extensions.select_users.buildSelectedItemStyle
import com.sceyt.chatuikit.styles.extensions.select_users.buildSeparatorTextStyle

typealias UsersListItemsStyle = SelectableListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, AvatarRenderer<SceytUser>>
typealias SelectedUsersListItemStyle = SelectedListItemStyle<Formatter<SceytUser>, AvatarRenderer<SceytUser>>


data class SelectUsersStyle(
        @ColorInt val backgroundColor: Int,
        val separatorText: String,
        val separatorTextStyle: TextStyle,
        val toolbarStyle: SearchToolbarStyle,
        val actionButton: ButtonStyle,
        val itemStyle: UsersListItemsStyle,
        val selectedItemStyle: SelectedUsersListItemStyle,
) {

    companion object {
        var styleCustomizer = StyleCustomizer<SelectUsersStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): SelectUsersStyle {
            context.obtainStyledAttributes(attrs, R.styleable.SelectUsers).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                val separatorText = context.getString(R.string.sceyt_users)

                return SelectUsersStyle(
                    backgroundColor = backgroundColor,
                    separatorText = separatorText,
                    separatorTextStyle = buildSeparatorTextStyle(array),
                    toolbarStyle = buildSearchToolbarStyle(array),
                    actionButton = buildActionButtonStyle(array),
                    itemStyle = buildItemStyle(array),
                    selectedItemStyle = buildSelectedItemStyle(array),
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}