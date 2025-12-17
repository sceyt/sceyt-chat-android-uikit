package com.sceyt.chatuikit.styles.channel_info.common_groups

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.buildCommonGroupMembersCountStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.buildCommonGroupTitleStyle

data class CommonGroupItemStyle(
    @param:ColorInt val backgroundColor: Int,
    val avatarStyle: AvatarStyle,
    val commonGroupTitleStyle: TextStyle,
    val commonGroupMembersCountStyle: TextStyle,
    val commonGroupTitleFormatter: Formatter<SceytChannel>,
    val commonGroupMembersCountFormatter: Formatter<SceytChannel>,
    val channelAvatarRenderer: AvatarRenderer<SceytChannel>,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<CommonGroupItemStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            internal val typedArray: TypedArray
    ) {
        fun build(): CommonGroupItemStyle {
            return CommonGroupItemStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
                avatarStyle = buildAvatarStyle(),
                commonGroupTitleStyle = buildCommonGroupTitleStyle(),
                commonGroupMembersCountStyle = buildCommonGroupMembersCountStyle(),
                commonGroupTitleFormatter = SceytChatUIKit.formatters.commonGroupTitleFormatter,
                commonGroupMembersCountFormatter = SceytChatUIKit.formatters.commonGroupMembersCountFormatter,
                channelAvatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}