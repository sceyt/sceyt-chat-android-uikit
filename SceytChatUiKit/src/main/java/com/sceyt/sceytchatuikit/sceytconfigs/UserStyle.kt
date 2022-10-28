package com.sceyt.sceytchatuikit.sceytconfigs

import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.sceytconfigs.dateformaters.UserPresenceDateFormatter

object UserStyle {

    @DrawableRes
    var userDefaultAvatar: Int = 0

    var avatarColors = arrayOf("#FF3E74", "#4F6AFF", "#FBB019", "#00CC99", "#9F35E7", "#63AFFF")

    var userPresenceDateFormat = UserPresenceDateFormatter()
}