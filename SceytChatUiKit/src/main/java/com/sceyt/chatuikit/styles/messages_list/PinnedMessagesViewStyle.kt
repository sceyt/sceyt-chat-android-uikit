package com.sceyt.chatuikit.styles.messages_list

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.attributes.PinnedMessageBodyFormatterAttributes
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.common.TextStyle

/**
 * Appearance of the pinned-messages banner shown under the toolbar.
 *
 * @property backgroundColor banner background.
 * @property separatorColor hairline between the banner and the message list.
 * @property pinIcon leading pin glyph.
 * @property indicatorActiveColor segment colour of the pin currently shown.
 * @property indicatorInactiveColor segment colour of the other pins.
 * @property titleTextStyle the "Pinned message" label.
 * @property bodyTextStyle the one-line message preview.
 * @property mentionTextStyle mentions inside the preview.
 * @property deletedStateText shown when the pinned message was deleted.
 * @property bodyFormatter builds the preview text.
 */
data class PinnedMessagesViewStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val separatorColor: Int,
    val pinIcon: Drawable?,
    @param:ColorInt val indicatorActiveColor: Int,
    @param:ColorInt val indicatorInactiveColor: Int,
    val titleTextStyle: TextStyle,
    val bodyTextStyle: TextStyle,
    val mentionTextStyle: TextStyle,
    val deletedStateText: CharSequence,
    val bodyFormatter: Formatter<PinnedMessageBodyFormatterAttributes>,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<PinnedMessagesViewStyle> { _, style -> style }
    }

    class Builder(
        private val context: Context,
        private val attributeSet: AttributeSet?,
    ) {

        fun build(): PinnedMessagesViewStyle {
            context.obtainStyledAttributes(attributeSet, R.styleable.PinnedMessagesView)
                .use { array ->
                    val backgroundColor = array.getColor(
                        R.styleable.PinnedMessagesView_sceytUiPinnedMessagesBackgroundColor,
                        context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
                    )
                    val separatorColor = array.getColor(
                        R.styleable.PinnedMessagesView_sceytUiPinnedMessagesSeparatorColor,
                        context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
                    )
                    val pinIcon = array.getDrawable(
                        R.styleable.PinnedMessagesView_sceytUiPinnedMessagesIcon
                    ) ?: context.getCompatDrawable(R.drawable.sceyt_ic_pin)
                    val pinIconTint = array.getColor(
                        R.styleable.PinnedMessagesView_sceytUiPinnedMessagesIconTint,
                        context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                    )
                    val indicatorActiveColor = array.getColor(
                        R.styleable.PinnedMessagesView_sceytUiPinnedMessagesIndicatorActiveColor,
                        context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                    )
                    val indicatorInactiveColor = array.getColor(
                        R.styleable.PinnedMessagesView_sceytUiPinnedMessagesIndicatorInactiveColor,
                        context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
                    )

                    val titleTextStyle = TextStyle(
                        color = array.getColor(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesTitleTextColor,
                            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                        ),
                        size = array.getDimensionPixelSize(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesTitleTextSize,
                            context.resources.getDimensionPixelSize(R.dimen.smallTextSize)
                        ),
                        style = array.getInt(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesTitleTextStyle,
                            Typeface.NORMAL
                        ),
                        font = array.getResourceId(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesTitleTextFont,
                            R.font.roboto_medium
                        )
                    )

                    val bodyTextStyle = TextStyle(
                        color = array.getColor(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesBodyTextColor,
                            context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
                        ),
                        size = array.getDimensionPixelSize(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesBodyTextSize,
                            context.resources.getDimensionPixelSize(R.dimen.smallTextSize)
                        ),
                        style = array.getInt(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesBodyTextStyle,
                            Typeface.NORMAL
                        ),
                        font = array.getResourceId(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesBodyTextFont,
                            R.font.roboto_regular
                        )
                    )

                    val mentionTextStyle = bodyTextStyle.copy(
                        color = array.getColor(
                            R.styleable.PinnedMessagesView_sceytUiPinnedMessagesMentionTextColor,
                            context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                        )
                    )

                    return PinnedMessagesViewStyle(
                        backgroundColor = backgroundColor,
                        separatorColor = separatorColor,
                        pinIcon = pinIcon?.mutate()?.apply { setTint(pinIconTint) },
                        indicatorActiveColor = indicatorActiveColor,
                        indicatorInactiveColor = indicatorInactiveColor,
                        titleTextStyle = titleTextStyle,
                        bodyTextStyle = bodyTextStyle,
                        mentionTextStyle = mentionTextStyle,
                        deletedStateText = context.getString(R.string.sceyt_message_was_deleted),
                        bodyFormatter = SceytChatUIKit.formatters.pinnedMessageBodyFormatter,
                    ).let { styleCustomizer.apply(context, it) }
                }
        }
    }
}