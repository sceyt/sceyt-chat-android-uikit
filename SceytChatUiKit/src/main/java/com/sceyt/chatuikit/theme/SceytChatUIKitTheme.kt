package com.sceyt.chatuikit.theme

import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R

data class SceytChatUIKitTheme(
        var colors: Colors = Colors(),
        @DrawableRes
        var userDefaultAvatar: Int = R.drawable.sceyt_ic_default_avatar,
        @DrawableRes
        var deletedUserAvatar: Int = R.drawable.sceyt_ic_deleted_user,
        @DrawableRes
        var notesAvatar: Int = R.drawable.sceyt_ic_notes,
)