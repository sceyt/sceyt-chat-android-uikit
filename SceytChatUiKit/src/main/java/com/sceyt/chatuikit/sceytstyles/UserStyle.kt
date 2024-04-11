package com.sceyt.chatuikit.sceytstyles

import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.sceytconfigs.dateformaters.UserPresenceDateFormatter

object UserStyle {

    @JvmField
    @DrawableRes
    var userDefaultAvatar: Int = R.drawable.sceyt_ic_default_avatar

    @JvmField
    @DrawableRes
    var deletedUserAvatar: Int = R.drawable.sceyt_ic_deleted_user

    @JvmField
    @DrawableRes
    var notesAvatar: Int = R.drawable.sceyt_ic_notes_with_paddings

    @JvmField
    var userPresenceDateFormat = UserPresenceDateFormatter()
}