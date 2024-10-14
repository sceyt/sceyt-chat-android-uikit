package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.providers.VisualProvider

open class DefaultUserAvatarProvider : VisualProvider<SceytUser, Drawable?> {
    override fun provide(context: Context, from: SceytUser): Drawable? {
        val resId = when (from.state) {
            UserState.Active -> R.drawable.sceyt_ic_default_avatar
            UserState.Inactive -> R.drawable.sceyt_ic_default_avatar
            UserState.Deleted -> R.drawable.sceyt_ic_deleted_user
        }
        return context.getCompatDrawable(resId)
    }
}