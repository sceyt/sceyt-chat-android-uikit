package com.sceyt.chatuikit.formatters

import com.sceyt.chatuikit.formatters.date.UserPresenceDateFormatter

class SceytChatUIKitFormatters {
    var userNameFormatter: UserNameFormatter? = null
        set(value) {
            field = value
            if (mentionUserNameFormatter == null)
                mentionUserNameFormatter = value
        }

    var mentionUserNameFormatter: UserNameFormatter? = null
    var userPresenceDateFormatter = UserPresenceDateFormatter()
    var avatarInitialsFormatter = DefaultAvatarInitialsFormatter()
    var messageDateSeparatorFormatter = DefaultMessageDateSeparatorFormatter()
}