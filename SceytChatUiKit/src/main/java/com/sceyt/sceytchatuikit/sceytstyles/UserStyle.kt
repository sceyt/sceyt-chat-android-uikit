package com.sceyt.sceytchatuikit.sceytstyles

import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.UserPresenceDateFormatter

object UserStyle {

    @JvmField
    @DrawableRes
    var userDefaultAvatar: Int = R.drawable.sceyt_ic_default_avatar

    @JvmField
    @DrawableRes
    var deletedUserAvatar: Int = R.drawable.sceyt_ic_deleted_user

    @JvmField
    var userPresenceDateFormat = UserPresenceDateFormatter()
}