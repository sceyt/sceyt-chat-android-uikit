package com.sceyt.chatuikit.renderers.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle

open class DefaultSuggestionUserAvatarRenderer : AvatarRenderer<SceytUser> {

    override fun render(
        context: Context,
        from: SceytUser,
        style: AvatarStyle,
        avatarView: AvatarView
    ) {
        avatarView.appearanceBuilder()
            .setStyle(style)
            .setDefaultAvatar(R.drawable.sceyt_ic_default_avatars_selected_user)
            .setImageUrl(from.avatarURL)
            .build()
            .applyToAvatar()
    }
}