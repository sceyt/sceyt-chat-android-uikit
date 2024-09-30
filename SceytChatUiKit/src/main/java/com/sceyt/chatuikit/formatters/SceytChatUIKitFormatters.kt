package com.sceyt.chatuikit.formatters

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.defaults.DefaultAttachmentNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAttachmentSizeFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelLastMessageSenderNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelSubtitleFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelUnreadCountFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMediaDurationFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMentionUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateSeparatorFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageViewCountFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserPresenceDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserShortNameFormatter
import com.sceyt.chatuikit.persistence.lazyVar
import java.util.Date

class SceytChatUIKitFormatters {
    //Todo: Remove this formatter
    var userNameFormatter: UserNameFormatter? = null
    val mentionUserNameFormatter: UserNameFormatter? = null

    var userPresenceDateFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserPresenceDateFormatter()
    }

    var userNameFormatterNew: Formatter<SceytUser> by lazyVar {
        DefaultUserNameFormatter
    }

    var userShortNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserShortNameFormatter
    }

    var mentionUserNameFormatterNew: Formatter<SceytUser> by lazyVar {
        DefaultMentionUserNameFormatter
    }

    var typingUserNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserShortNameFormatter
    }

    var reactedUserNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserNameFormatter
    }

    var channelNameFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelNameFormatter
    }

    var channelSubtitleFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelSubtitleFormatter
    }

    var channelDateFormatter: Formatter<Date> by lazyVar {
        DefaultChannelDateFormatter()
    }

    var channelUnreadCountFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelUnreadCountFormatter
    }

    var channelLastMessageSenderNameFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelLastMessageSenderNameFormatter
    }

    var messageDateFormatter: Formatter<Date> by lazyVar {
        DefaultMessageDateFormatter
    }

    var messageDateSeparatorFormatter: Formatter<Date> by lazyVar {
        DefaultMessageDateSeparatorFormatter()
    }

    var messageViewCountFormatter: Formatter<Long> by lazyVar {
        DefaultMessageViewCountFormatter
    }

    var attachmentNameFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultAttachmentNameFormatter
    }

    var mediaDurationFormatter: Formatter<Long> by lazyVar {
        DefaultMediaDurationFormatter
    }

    var attachmentSizeFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultAttachmentSizeFormatter
    }
}