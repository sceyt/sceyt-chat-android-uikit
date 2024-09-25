package com.sceyt.chatuikit.formatters

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.date.UserPresenceDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAvatarInitialsFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMentionUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageBodyFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateSeparatorFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultTypingUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserNameFormatter
import com.sceyt.chatuikit.persistence.lazyVar
import java.util.Date

class SceytChatUIKitFormatters {
    var userNameFormatter: UserNameFormatter? = null
    val mentionUserNameFormatter:UserNameFormatter? = null

    var userNameFormatterNew: Formatter<User> by lazyVar {
        DefaultUserNameFormatter()
    }

    var mentionUserNameFormatterNew: Formatter<User> by lazyVar {
        DefaultMentionUserNameFormatter()
    }

    var messageSenderNameFormatter: Formatter<User> by lazyVar {
        DefaultUserNameFormatter()
    }

    var typingUserNameFormatter: Formatter<User> by lazyVar {
        DefaultTypingUserNameFormatter()
    }

    var reactedUserNameFormatter: Formatter<User> by lazyVar {
        DefaultUserNameFormatter()
    }

    var userPresenceDateFormatter: UserPresenceDateFormatter by lazyVar {
        UserPresenceDateFormatter()
    }

    var channelNameFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelNameFormatter()
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