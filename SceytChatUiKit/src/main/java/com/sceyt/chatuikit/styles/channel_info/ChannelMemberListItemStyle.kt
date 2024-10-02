package com.sceyt.chatuikit.styles.channel_info

import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.ListItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import java.util.Date

data class ChannelMemberListItemStyle(
        var roleTextStyle: TextStyle,
        var listItemStyle: ListItemStyle<Formatter<SceytUser>, Formatter<Date>, VisualProvider<SceytUser, DefaultAvatar>>
)