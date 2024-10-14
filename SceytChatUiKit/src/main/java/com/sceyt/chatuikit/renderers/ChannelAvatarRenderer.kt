package com.sceyt.chatuikit.renderers

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.common.AvatarStyle

fun interface ChannelAvatarRenderer : VisualRenderer {
    fun render(context: Context, channel: SceytChannel, style: AvatarStyle, avatarView: AvatarView)
}