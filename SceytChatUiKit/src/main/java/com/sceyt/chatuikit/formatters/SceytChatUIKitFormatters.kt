package com.sceyt.chatuikit.formatters

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.date.UserPresenceDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAvatarInitialsFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageBodyFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateSeparatorFormatter
import com.sceyt.chatuikit.persistence.lazyVar
import java.util.Date

class SceytChatUIKitFormatters {
    var userNameFormatter: UserNameFormatter? = null
        set(value) {
            field = value
            if (mentionUserNameFormatter == null)
                mentionUserNameFormatter = value
        }

    var mentionUserNameFormatter: UserNameFormatter? = null

    var userPresenceDateFormatter: UserPresenceDateFormatter by lazyVar {
        UserPresenceDateFormatter()
    }

    var channelDateFormatter: Formatter<Date> by lazyVar {
        DefaultChannelDateFormatter()
    }

    var messageBodyFormatter: Formatter<SceytMessage> by lazyVar {
        DefaultMessageBodyFormatter()
    }

    var messageDateSeparatorFormatter: Formatter<Date> by lazyVar {
        DefaultMessageDateSeparatorFormatter()
    }

    var avatarInitialsFormatter: Formatter<String> by lazyVar {
        DefaultAvatarInitialsFormatter()
    }
}