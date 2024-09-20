package com.sceyt.chatuikit.providers

import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.presentation.customviews.AvatarView
import com.sceyt.chatuikit.presentation.customviews.toDefaultAvatar

interface UserDefaultAvatarProvider : Provider<User, AvatarView.DefaultAvatar>

open class DefaultUserAvatarProvider : UserDefaultAvatarProvider {
    override fun provide(from: User): AvatarView.DefaultAvatar {
        return (when (from.activityState ?: UserState.Active) {
            UserState.Active -> R.drawable.sceyt_ic_default_avatar
            UserState.Inactive -> R.drawable.sceyt_ic_default_avatar
            UserState.Deleted -> R.drawable.sceyt_ic_deleted_user
        }).toDefaultAvatar()
    }
}