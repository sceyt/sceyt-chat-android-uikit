package com.sceyt.chatuikit.presentation.extensions

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView

fun AvatarView.setUserAvatar(
        user: SceytUser?
) {
    if (user == null) {
        setImageUrl(null)
        return
    }
    appearanceBuilder()
        .setDefaultAvatar(SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, user))
        .setImageUrl(user.avatarURL)
        .build()
        .applyToAvatar()
}