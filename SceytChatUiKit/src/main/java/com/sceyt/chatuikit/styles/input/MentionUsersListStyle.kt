package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.NoFormatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class MentionUsersListStyle(
        @param:ColorInt val backgroundColor: Int,
        val itemStyle: ListItemStyle<Formatter<SceytUser>, *, AvatarRenderer<SceytUser>>
) {
    companion object {
        var styleCustomizer = StyleCustomizer<MentionUsersListStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var titleTextStyle: TextStyle = TextStyle()

        private val itemStyle: ListItemStyle<Formatter<SceytUser>, *, AvatarRenderer<SceytUser>> by lazy {
            ListItemStyle(
                titleTextStyle = titleTextStyle,
                titleFormatter = SceytChatUIKit.formatters.userNameFormatter,
                subtitleFormatter = NoFormatter,
                avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer
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
        ).let { styleCustomizer.apply(context, it) }
    }
}
