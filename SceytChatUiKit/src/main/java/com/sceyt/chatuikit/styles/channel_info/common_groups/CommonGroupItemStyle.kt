package com.sceyt.chatuikit.styles.channel_info.common_groups

import android.content.Context
import android.content.res.TypedArray
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelSubtitleFormatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.buildAvatarStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.buildCommonGroupMembersCountStyle
import com.sceyt.chatuikit.styles.extensions.channel_info.common_groups.buildCommonGroupTitleStyle

data class CommonGroupItemStyle(
    val avatarStyle: AvatarStyle,
    val titleStyle: TextStyle,
    val membersCountStyle: TextStyle,
    val titleFormatter: Formatter<SceytChannel>,
    val membersCountFormatter: Formatter<SceytChannel>,
    val avatarRenderer: AvatarRenderer<SceytChannel>,
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
                avatarStyle = buildAvatarStyle(),
                titleStyle = buildCommonGroupTitleStyle(),
                membersCountStyle = buildCommonGroupMembersCountStyle(),
                titleFormatter = DefaultChannelNameFormatter(),
                membersCountFormatter = DefaultChannelSubtitleFormatter(),
                avatarRenderer = SceytChatUIKit.renderers.channelAvatarRenderer
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}