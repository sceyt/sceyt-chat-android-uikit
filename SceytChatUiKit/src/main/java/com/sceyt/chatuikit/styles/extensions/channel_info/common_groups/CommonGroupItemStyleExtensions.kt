package com.sceyt.chatuikit.styles.extensions.channel_info.common_groups

import android.graphics.Typeface
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.channel_info.common_groups.CommonGroupItemStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle

internal fun buildAvatarStyle() = AvatarStyle()

internal fun CommonGroupItemStyle.Builder.buildCommonGroupTitleStyle() = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupTitleTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
    )
    .setStyle(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupTitleTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupTitleTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun CommonGroupItemStyle.Builder.buildCommonGroupMembersCountStyle() = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupMembersCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupMembersCountTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.extraSmallTextSize)
    )
    .setStyle(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupMembersCountTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelInfoCommonGroupsView_sceytUiCommonGroupMembersCountTextFont,
        defValue = R.font.roboto_regular
    )
    .build()