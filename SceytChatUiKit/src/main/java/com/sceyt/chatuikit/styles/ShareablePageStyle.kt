package com.sceyt.chatuikit.styles

import androidx.annotation.ColorInt
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.SearchToolbarStyle
import com.sceyt.chatuikit.styles.common.SelectableListItemStyle

abstract class ShareablePageStyle(
        @ColorInt open val backgroundColor: Int,
        open val searchToolbarStyle: SearchToolbarStyle,
        open val actionButtonStyle: ButtonStyle,
        open val channelItemStyle: SelectableListItemStyle<
                Formatter<SceytChannel>,
                Formatter<SceytChannel>,
                VisualProvider<SceytChannel, AvatarView.DefaultAvatar>>,
)