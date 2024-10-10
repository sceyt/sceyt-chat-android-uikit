package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.toDefaultAvatar
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultUserAvatarProvider : VisualProvider<SceytUser, AvatarView.DefaultAvatar> {
    override fun provide(context: Context, from: SceytUser): AvatarView.DefaultAvatar {
        return (when (from.state) {
            UserState.Active -> R.drawable.sceyt_ic_default_avatar
            UserState.Inactive -> R.drawable.sceyt_ic_default_avatar
            UserState.Deleted -> R.drawable.sceyt_ic_deleted_user
        }).toDefaultAvatar()
    }
}