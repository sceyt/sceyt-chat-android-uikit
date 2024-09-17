package com.sceyt.chatuikit.formatters

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