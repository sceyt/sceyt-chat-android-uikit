package com.sceyt.chatuikit.styles.select_users

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
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
import com.sceyt.chatuikit.theme.Colors

typealias UsersListItemsStyle = SelectableListItemStyle<Formatter<SceytUser>, Formatter<SceytUser>, AvatarRenderer<SceytUser>>
typealias SelectedUsersListItemStyle = SelectedListItemStyle<Formatter<SceytUser>, AvatarRenderer<SceytUser>>

/**
 * Style for the [SelectUsersActivity].
 * @param backgroundColor Background color of the screen. Default is [Colors.backgroundColor].
 * @param separatorText Text for the separator. Default is [R.string.sceyt_users].
 * @param separatorTextStyle Style for the separator text. Default is [buildSeparatorTextStyle].
 * @param toolbarStyle Style for the toolbar. Default is [buildSearchToolbarStyle].
 * @param actionButton Style for the action button. Default is [buildActionButtonStyle].
 * @param itemStyle Style for the items in the list. Default is [buildItemStyle].
 * @param selectedItemStyle Style for the selected items. Default is [buildSelectedItemStyle].
 * */
data class SelectUsersStyle(
        @param:ColorInt val backgroundColor: Int,
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