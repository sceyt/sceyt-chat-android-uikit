package com.sceyt.chatuikit

import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytconfigs.dateformaters.UserPresenceDateFormatter

class SceytChatUIKitFormatters {
    var userNameFormatter: UserNameFormatter? = null
        set(value) {
            field = value
            if (mentionUserNameFormatter == null)
                mentionUserNameFormatter = value
        }

    var mentionUserNameFormatter: UserNameFormatter? = null
    var userPresenceDateFormatter = UserPresenceDateFormatter()
}