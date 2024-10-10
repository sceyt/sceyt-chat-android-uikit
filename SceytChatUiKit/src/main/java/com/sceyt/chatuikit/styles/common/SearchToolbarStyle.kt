package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.presentation.custom_views.SearchableToolbar

data class SearchToolbarStyle(
        val toolbarStyle: ToolbarStyle,
        val searchInputStyle: SearchInputStyle
) {
    fun apply(toolbar: SearchableToolbar) {
        with(toolbarStyle) {
            toolbar.setBackgroundColor(backgroundColor)
            toolbar.setTitleTextStyle(titleTextStyle)
            toolbar.setBorderColor(underlineColor)
            toolbar.setNavigationIcon(navigationIcon)
            toolbar.setSearchInputStyle(searchInputStyle)
        }
    }
}