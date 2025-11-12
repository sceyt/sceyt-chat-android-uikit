package com.sceyt.chatuikit.renderers.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle

open class DefaultUserAvatarRenderer : AvatarRenderer<SceytUser> {

    override fun render(
        context: Context,
        from: SceytUser,
        style: AvatarStyle,
        avatarView: AvatarView
    ) {
        val defaultAvatar =
            SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, from)
        avatarView.appearanceBuilder()
            .setStyle(style)
            .setDefaultAvatar(defaultAvatar)
            .setImageUrl(from.avatarURL)
            .build()
            .applyToAvatar()
    }
}