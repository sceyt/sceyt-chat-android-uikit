package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class GlobalSearchStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:ColorInt val dividerColor: Int,
    @param:ColorInt val searchInputBackgroundColor: Int,
    @param:ColorInt val navigationIconColor: Int,
    @param:ColorInt val searchIconColor: Int,
    @param:ColorInt val clearIconColor: Int,
    @param:ColorInt val searchHintColor: Int,
    @param:ColorInt val tabSelectedBackgroundColor: Int,
    @param:ColorInt val tabSelectedTextColor: Int,
    @param:ColorInt val tabUnselectedBackgroundColor: Int,
    @param:ColorInt val tabUnselectedTextColor: Int,
    @param:ColorInt val tabStrokeColor: Int,
    @param:ColorInt val chipBackgroundColor: Int,
    @param:ColorInt val chipTextColor: Int,
    @param:ColorInt val chipStrokeColor: Int,
    @param:ColorInt val suggestionChipBackgroundColor: Int,
    @param:ColorInt val suggestionChipTextColor: Int,
    @param:ColorInt val suggestionChipIconColor: Int,
    @param:ColorInt val selectedMemberChipBackgroundColor: Int,
    @param:ColorInt val selectedMemberChipTextColor: Int,
    @param:ColorInt val selectedMemberChipIconColor: Int,
    @param:ColorInt val selectedMemberChipPendingBackgroundColor: Int,
    @param:ColorInt val selectedMemberChipPendingTextColor: Int,
    @param:ColorInt val selectedMemberChipPendingIconColor: Int,
    @param:ColorInt val highlightTextColor: Int,
    val titleTextStyle: TextStyle,
    val subtitleTextStyle: TextStyle,
    val metaTextStyle: TextStyle,
    val sectionTextStyle: TextStyle,
    val emptyTitleTextStyle: TextStyle,
    val emptySubtitleTextStyle: TextStyle,
    val avatarStyle: AvatarStyle,
    val channelItemStyle: ChannelItemStyle,
) : SceytComponentStyle() {

    internal class Builder(
        private val context: Context,
    ) {
        fun build(): GlobalSearchStyle {
            val colors = SceytChatUIKit.theme.colors
            return GlobalSearchStyle(
                backgroundColor = context.getCompatColor(colors.backgroundColor),
                dividerColor = context.getCompatColor(colors.borderColor),
                searchInputBackgroundColor = context.getCompatColor(colors.surface1Color),
                navigationIconColor = context.getCompatColor(colors.accentColor),
                searchIconColor = context.getCompatColor(colors.iconSecondaryColor),
                clearIconColor = context.getCompatColor(colors.iconSecondaryColor),
                searchHintColor = context.getCompatColor(colors.textFootnoteColor),
                tabSelectedBackgroundColor = context.getCompatColor(colors.surface1Color),
                tabSelectedTextColor = context.getCompatColor(colors.textPrimaryColor),
                tabUnselectedBackgroundColor = context.getCompatColor(colors.backgroundColor),
                tabUnselectedTextColor = context.getCompatColor(colors.textSecondaryColor),
                tabStrokeColor = context.getCompatColor(colors.borderColor),
                chipBackgroundColor = context.getCompatColor(colors.backgroundColor),
                chipTextColor = context.getCompatColor(colors.textSecondaryColor),
                chipStrokeColor = context.getCompatColor(colors.borderColor),
                suggestionChipBackgroundColor = context.getCompatColor(colors.surface1Color),
                suggestionChipTextColor = context.getCompatColor(colors.textPrimaryColor),
                suggestionChipIconColor = context.getCompatColor(colors.textSecondaryColor),
                selectedMemberChipBackgroundColor = context.getCompatColor(colors.surface3Color),
                selectedMemberChipTextColor = context.getCompatColor(colors.onPrimaryColor),
                selectedMemberChipIconColor = context.getCompatColor(colors.onPrimaryColor),
                selectedMemberChipPendingBackgroundColor = "#474F8C".toColorInt(),
                selectedMemberChipPendingTextColor = context.getCompatColor(colors.onPrimaryColor),
                selectedMemberChipPendingIconColor = context.getCompatColor(colors.onPrimaryColor),
                highlightTextColor = context.getCompatColor(colors.accentColor),
                titleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textPrimaryColor),
                    font = R.font.roboto_medium
                ),
                subtitleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textPrimaryColor),
                    font = R.font.roboto_regular
                ),
                metaTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textSecondaryColor),
                    font = R.font.roboto_regular
                ),
                sectionTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textSecondaryColor),
                    font = R.font.roboto_medium
                ),
                emptyTitleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textPrimaryColor),
                    font = R.font.roboto_medium
                ),
                emptySubtitleTextStyle = TextStyle(
                    color = context.getCompatColor(colors.textSecondaryColor),
                    font = R.font.roboto_regular
                ),
                avatarStyle = AvatarStyle(),
                channelItemStyle = ChannelItemStyle.Builder(context, null).build()
            )
        }
    }
}
