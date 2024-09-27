package com.sceyt.chatuikit.formatters

import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.defaults.DefaultMediaDurationFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAttachmentNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAttachmentSizeFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAvatarInitialsFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelLastMessageSenderNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelSubtitleFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelUnreadCountFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMentionUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageBodyFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateSeparatorFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserPresenceDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserShortNameFormatter
import com.sceyt.chatuikit.persistence.lazyVar
import java.util.Date

class SceytChatUIKitFormatters {
    //Todo: Remove this formatter
    var userNameFormatter: UserNameFormatter? = null
    val mentionUserNameFormatter: UserNameFormatter? = null

    var userPresenceDateFormatter: Formatter<User> by lazyVar {
        DefaultUserPresenceDateFormatter()
    }

    var userNameFormatterNew: Formatter<User> by lazyVar {
        DefaultUserNameFormatter()
    }

    var userShortNameFormatter: Formatter<User> by lazyVar {
        DefaultUserShortNameFormatter()
    }

    var mentionUserNameFormatterNew: Formatter<User> by lazyVar {
        DefaultMentionUserNameFormatter()
    }

    var typingUserNameFormatter: Formatter<User> by lazyVar {
        DefaultUserShortNameFormatter()
    }

    var reactedUserNameFormatter: Formatter<User> by lazyVar {
        DefaultUserNameFormatter()
    }

    var channelNameFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelNameFormatter()
    }

    var channelSubtitleFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelSubtitleFormatter()
    }

    var channelDateFormatter: Formatter<Date> by lazyVar {
        DefaultChannelDateFormatter()
    }

    var channelUnreadCountFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelUnreadCountFormatter()
    }

    var channelLastMessageSenderNameFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelLastMessageSenderNameFormatter()
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

    var attachmentNameFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultAttachmentNameFormatter()
    }

    var mediaDurationFormatter: Formatter<Long> by lazyVar {
        DefaultMediaDurationFormatter()
    }

    var attachmentSizeFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultAttachmentSizeFormatter()
    }
}