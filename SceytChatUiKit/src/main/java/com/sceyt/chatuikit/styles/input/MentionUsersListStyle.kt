package com.sceyt.chatuikit.styles.input

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.NoFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserNameFormatter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.providers.defaults.DefaultUserAvatarProvider
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class MentionUsersListStyle(
        @ColorInt var backgroundColor: Int,
        var itemStyle: ListItemStyle<Formatter<SceytUser>, *, VisualProvider<SceytUser, DefaultAvatar>>
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var titleTextStyle: TextStyle = TextStyle()

        private val itemStyle: ListItemStyle<Formatter<SceytUser>, *, VisualProvider<SceytUser, DefaultAvatar>> by lazy {
            ListItemStyle(
                titleTextStyle = titleTextStyle,
                titleFormatter = DefaultUserNameFormatter,
                subtitleFormatter = NoFormatter,
                avatarProvider = DefaultUserAvatarProvider
            )
        }

        fun backgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun titleTextStyle(titleTextStyle: TextStyle) = apply {
            this.titleTextStyle = titleTextStyle
        }

        fun build() = MentionUsersListStyle(
            backgroundColor = backgroundColor,
            itemStyle = itemStyle
        )
    }
}
