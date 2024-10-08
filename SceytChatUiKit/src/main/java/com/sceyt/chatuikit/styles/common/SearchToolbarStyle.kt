package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.presentation.custom_views.SearchableToolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class SearchToolbarStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val borderColor: Int = UNSET_COLOR,
        val navigationIcon: Drawable? = null,
        val titleTextStyle: TextStyle = TextStyle(),
        val searchInputStyle: SearchInputStyle = SearchInputStyle()
) {

    fun apply(toolbar: SearchableToolbar) {
        toolbar.setBackgroundColor(backgroundColor)
        toolbar.setTitleTextStyle(titleTextStyle)
        toolbar.setBorderColor(borderColor)
        toolbar.setNavigationIcon(navigationIcon)
    }
}