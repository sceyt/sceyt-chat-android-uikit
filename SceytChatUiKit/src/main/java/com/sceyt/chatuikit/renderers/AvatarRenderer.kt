package com.sceyt.chatuikit.renderers

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.common.AvatarStyle

typealias UserAvatarRenderer = AvatarRenderer<SceytUser>
typealias ChannelAvatarRenderer = AvatarRenderer<SceytChannel>

interface AvatarRenderer <T>: VisualRenderer {
    fun render(context: Context, from: T, style: AvatarStyle, avatarView: AvatarView)
}