package com.sceyt.chatuikit.renderers.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle

open class DefaultUserAndNotesAvatarRenderer : AvatarRenderer<SceytUser> {

    override fun render(context: Context, from: SceytUser, style: AvatarStyle, avatarView: AvatarView) {

        val defaultAvatar = SceytChatUIKit.providers.userDefaultAvatarProvider.provide(context, from)
        val builder = avatarView.appearanceBuilder()
            .setStyle(style)
            .setDefaultAvatar(defaultAvatar)
            .setImageUrl(from.avatarURL)

        if (from.id == SceytChatUIKit.currentUserId) {
            builder.setImageUrl(null)
            builder.setDefaultAvatar(
                drawable = context
                    .getCompatDrawable(R.drawable.sceyt_ic_notes_with_bachgriund_layers)
                    .applyTintBackgroundLayer(
                        context = context,
                        tintColor = SceytChatUIKit.theme.colors.accentColor,
                        bgLayerId = R.id.backgroundLayer
                    ))
        }

        builder.build().applyToAvatar()
    }
}