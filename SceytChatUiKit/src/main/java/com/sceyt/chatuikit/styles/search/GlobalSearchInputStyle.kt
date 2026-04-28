package com.sceyt.chatuikit.styles.search

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
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildChipBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildChipTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSearchInputStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSelectedUserChipBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSelectedUserChipPendingBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSelectedUserChipPendingTextStyle
import com.sceyt.chatuikit.styles.extensions.search.buildSelectedUserChipTextStyle

data class GlobalSearchInputStyle(
    val searchInputStyle: SearchInputStyle,
    val chipBackgroundStyle: BackgroundStyle,
    val chipTextStyle: TextStyle,
    val selectedUserChipBackgroundStyle: BackgroundStyle,
    val selectedUserChipTextStyle: TextStyle,
    @param:ColorInt val selectedUserChipIconColor: Int,
    val selectedUserChipPendingBackgroundStyle: BackgroundStyle,
    val selectedUserChipPendingTextStyle: TextStyle,
    @param:ColorInt val selectedUserChipPendingIconColor: Int,
    val avatarStyle: AvatarStyle,
    val userNameFormatter: Formatter<SceytUser>,
    val avatarRenderer: AvatarRenderer<SceytUser>,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<GlobalSearchInputStyle> { _, style -> style }
    }

    internal class Builder(
        internal val context: Context,
        internal val attrs: AttributeSet? = null,
    ) {
        fun build(): GlobalSearchInputStyle =
            context.obtainStyledAttributes(attrs, R.styleable.GlobalSearchInputView)
                .use { typedArray ->
                    val colors = SceytChatUIKit.theme.colors
                    return GlobalSearchInputStyle(
                        searchInputStyle = buildSearchInputStyle(),
                        chipBackgroundStyle = buildChipBackgroundStyle(),
                        chipTextStyle = buildChipTextStyle(),
                        selectedUserChipBackgroundStyle = buildSelectedUserChipBackgroundStyle(),
                        selectedUserChipTextStyle = buildSelectedUserChipTextStyle(),
                        selectedUserChipIconColor = context.getCompatColor(colors.onPrimaryColor),
                        selectedUserChipPendingBackgroundStyle = buildSelectedUserChipPendingBackgroundStyle(),
                        selectedUserChipPendingTextStyle = buildSelectedUserChipPendingTextStyle(),
                        selectedUserChipPendingIconColor = context.getCompatColor(colors.onPrimaryColor),
                        avatarStyle = AvatarStyle.Builder(typedArray).build(),
                        userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                        avatarRenderer = SceytChatUIKit.renderers.suggestionUserAvatarRenderer,
                    ).let { styleCustomizer.apply(context, it) }
                }
    }
}
