package com.sceyt.chatuikit.styles.search

import android.content.Context
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSelectedTabBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSelectedTabTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildUnselectedTabBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildUnselectedTabTextStyle

data class GlobalSearchTabBarStyle(
    val selectedTabBackgroundStyle: BackgroundStyle,
    val selectedTabTextStyle: TextStyle,
    val unselectedTabBackgroundStyle: BackgroundStyle,
    val unselectedTabTextStyle: TextStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<GlobalSearchTabBarStyle> { _, style -> style }
    }

    internal class Builder(internal val context: Context) {
        fun build(): GlobalSearchTabBarStyle {
            return GlobalSearchTabBarStyle(
                selectedTabBackgroundStyle = buildSelectedTabBackgroundStyle(),
                selectedTabTextStyle = buildSelectedTabTextStyle(),
                unselectedTabBackgroundStyle = buildUnselectedTabBackgroundStyle(),
                unselectedTabTextStyle = buildUnselectedTabTextStyle(),
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
