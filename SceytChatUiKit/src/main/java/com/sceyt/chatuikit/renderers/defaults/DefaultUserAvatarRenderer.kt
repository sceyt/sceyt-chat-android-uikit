package com.sceyt.chatuikit.renderers.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.renderers.UserAvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle

open class DefaultUserAvatarRenderer : UserAvatarRenderer {

    override fun render(context: Context, user: SceytUser, style: AvatarStyle, avatarView: AvatarView) {

        val defaultAvatar = SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, user)

        avatarView.appearanceBuilder()
            .setStyle(style)
            .setDefaultAvatar(defaultAvatar)
            .build()
            .applyToAvatar()
    }
}