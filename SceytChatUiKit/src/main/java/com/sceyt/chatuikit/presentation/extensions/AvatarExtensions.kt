package com.sceyt.chatuikit.presentation.extensions

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.AvatarView.DefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider

fun AvatarView.setChannelAvatar(
        channel: SceytChannel,
        defaultAvatarProvider: VisualProvider<SceytChannel, DefaultAvatar>
        = SceytChatUIKit.providers.channelDefaultAvatarProvider,
        isSelf: Boolean = channel.isSelf()
) {
    val defaultAvatar = defaultAvatarProvider.provide(channel)
    val builder = styleBuilder()
        .setDefaultAvatar(defaultAvatar)

    if (isSelf || channel.isPeerDeleted()) {
        builder.setImageUrl(null)
    } else builder.setImageUrl(channel.iconUrl)

    builder.setAvatarBackgroundColorRes(if (isSelf) SceytChatUIKit.theme.accentColor else 0)
    builder.build()
}

fun AvatarView.setUserAvatar(user: User?) {
    if (user == null) {
        setImageUrl(null)
        return
    }
    val defaultAvatar = SceytChatUIKit.providers.userDefaultAvatarProvider.provide(user)
    styleBuilder()
        .setDefaultAvatar(defaultAvatar)
        .setAvatarBackgroundColor(0)
        .setImageUrl(user.avatarURL)
        .build()
}