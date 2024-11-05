@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.select_users

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.SelectUsersStyle
import com.sceyt.chatuikit.styles.SelectedUsersListItemStyle
import com.sceyt.chatuikit.styles.UsersListItemsStyle

internal fun SelectUsersStyle.Builder.buildSeparatorTextStyle(
        array: TypedArray,
) = TextStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color),
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_medium
)

internal fun SelectUsersStyle.Builder.buildSearchInputStyle(
        array: TypedArray,
) = SearchInputStyle(
    searchIcon = context.getCompatDrawable(R.drawable.sceyt_ic_search).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    ),
    clearIcon = context.getCompatDrawable(R.drawable.sceyt_ic_cancel).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
    ),
    textInputStyle = TextInputStyle(
        hintStyle = HintStyle(
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor),
            hint = context.getString(R.string.sceyt_search)
        ),
        textStyle = TextStyle(
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
        )
    )
)

internal fun SelectUsersStyle.Builder.buildSearchToolbarStyle(
        array: TypedArray,
) = SearchToolbarStyle(
    toolbarStyle = ToolbarStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
        underlineColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
        navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
        ),
        titleTextStyle = TextStyle(
            color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
            font = R.font.roboto_medium
        )
    ),
    searchInputStyle = buildSearchInputStyle(array)
)

internal fun SelectUsersStyle.Builder.buildActionButtonStyle(
        array: TypedArray,
) = ButtonStyle(
    backgroundStyle = BackgroundStyle(
        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor),
    ),
    icon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_next).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
)

internal fun SelectUsersStyle.Builder.buildItemStyle(
        array: TypedArray,
) = UsersListItemsStyle(
    checkboxStyle = CheckboxStyle.default(context),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    ),
    subtitleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    ),
    titleFormatter = SceytChatUIKit.formatters.userNameFormatter,
    subtitleFormatter = SceytChatUIKit.formatters.userPresenceDateFormatter,
    avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer
)

internal fun SelectUsersStyle.Builder.buildSelectedItemStyle(
        array: TypedArray,
) = SelectedUsersListItemStyle(
    removeIcon = context.getCompatDrawable(R.drawable.sceyt_ic_remove),
    textStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    ),
    avatarStyle = AvatarStyle(),
    presenceStateColorProvider = SceytChatUIKit.providers.presenceStateColorProvider,
    nameFormatter = SceytChatUIKit.formatters.userNameFormatter,
    avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer
)
