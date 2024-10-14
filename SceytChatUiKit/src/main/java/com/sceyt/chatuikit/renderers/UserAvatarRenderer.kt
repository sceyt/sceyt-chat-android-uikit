package com.sceyt.chatuikit.renderers

import android.content.Context
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.common.AvatarStyle

interface UserAvatarRenderer : VisualRenderer {
    fun render(context: Context, user: SceytUser, style: AvatarStyle, avatarView: AvatarView)
}