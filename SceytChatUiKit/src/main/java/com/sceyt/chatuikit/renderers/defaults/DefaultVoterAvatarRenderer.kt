package com.sceyt.chatuikit.renderers.defaults

import android.content.Context
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.messages_list.item.VoterAvatarRendererAttributes

open class DefaultVoterAvatarRenderer : AvatarRenderer<VoterAvatarRendererAttributes> {

    override fun render(
            context: Context,
            from: VoterAvatarRendererAttributes,
            style: AvatarStyle,
            avatarView: AvatarView,
    ) {
        val defaultAvatar = SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, from.voter)
        avatarView.appearanceBuilder()
            .setStyle(style)
            .setDefaultAvatar(defaultAvatar)
            .setImageUrl(from.voter.avatarURL)
            .setBorder(5f, from.bubbleBackgroundStyle.backgroundColor)
            .build()
            .applyToAvatar()
    }
}