package com.sceyt.chatuikit.styles.pinned_messages

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setIconsTintColorRes
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.pinned_messages.buildToolbarStyle

/** Screen styles. Message cells use the conversation's MessagesListViewStyle. */
data class PinnedMessagesStyle(
    @param:ColorInt val backgroundColor: Int,
    val toolbarTitle: String,
    val toolbarStyle: ToolbarStyle,
    val navigateIcon: Drawable?,
    val navigateButtonBackgroundStyle: BackgroundStyle,
    val emptyStateText: String,
    val emptyStateTextStyle: TextStyle,
    val messageActionsMenuStyle: MenuStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<PinnedMessagesStyle> { _, style -> style }
    }

    class Builder(
        internal val context: Context,
        private val attrs: AttributeSet?,
    ) {
        fun build(): PinnedMessagesStyle {
            val colors = SceytChatUIKit.theme.colors
            return context.obtainStyledAttributes(attrs, R.styleable.PinnedMessages).use { array ->
                PinnedMessagesStyle(
                    backgroundColor = array.getColor(
                        R.styleable.PinnedMessages_sceytUiPinnedMessagesScreenBackgroundColor,
                        context.getCompatColor(colors.backgroundColor)
                    ),
                    toolbarTitle = array.getString(R.styleable.PinnedMessages_sceytUiToolbarTitle)
                        ?: context.getString(R.string.sceyt_pinned_messages),
                    toolbarStyle = buildToolbarStyle(array),
                    navigateIcon = array.getDrawable(R.styleable.PinnedMessages_sceytUiPinnedMessagesNavigateIcon)
                        ?: context.getCompatDrawable(R.drawable.sceyt_ic_pinned_message_navigate)
                            .applyTint(context, colors.iconSecondaryColor),
                    navigateButtonBackgroundStyle = BackgroundStyle(
                        backgroundColor = array.getColor(
                            R.styleable.PinnedMessages_sceytUiPinnedMessagesNavigateBackgroundColor,
                            context.getCompatColor(colors.surface1Color)
                        ),
                        shape = Shape.Circle
                    ),
                    emptyStateText = array.getString(R.styleable.PinnedMessages_sceytUiPinnedMessagesEmptyText)
                        ?: context.getString(R.string.sceyt_no_pinned_messages),
                    emptyStateTextStyle = TextStyle.Builder(array)
                        .setColor(
                            R.styleable.PinnedMessages_sceytUiPinnedMessagesEmptyTextColor,
                            context.getCompatColor(colors.textSecondaryColor)
                        )
                        .setSize(
                            R.styleable.PinnedMessages_sceytUiPinnedMessagesEmptyTextSize,
                            context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
                        )
                        .build(),
                    messageActionsMenuStyle = MenuStyle(
                        popupTheme = R.style.SceytPopupMenuStyle,
                        titleAppearance = R.style.SceytMenuTitleAppearance,
                        menuRes = R.menu.sceyt_menu_message_actions,
                        overFlowIcon = context.getCompatDrawable(R.drawable.sceyt_ic_more_24)
                            .applyTint(context, colors.accentColor),
                        menuCustomizer = { setIconsTintColorRes(context, colors.accentColor) }
                    )
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}