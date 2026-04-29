package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.presentation.custom_views.SearchableToolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class SearchToolbarStyle(
    val toolbarStyle: ToolbarStyle,
    val searchInputStyle: SearchInputStyle
) {
    fun apply(toolbar: SearchableToolbar) {
        with(toolbarStyle) {
            if (backgroundColor != UNSET_COLOR)
                toolbar.setBackgroundColor(backgroundColor)

            if (underlineColor != UNSET_COLOR)
                toolbar.setBorderColor(underlineColor)

            toolbar.setTitleTextStyle(titleTextStyle)
            toolbar.setNavigationIcon(navigationIcon)
            toolbar.setSearchInputStyle(searchInputStyle)
        }
    }
}