package com.sceyt.chatuikit.formatters

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.attributes.DraftMessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.MessageBodyFormatterAttributes
import com.sceyt.chatuikit.formatters.defaults.DefaultAttachmentNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultAttachmentSizeFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelInfoAttachmentDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelInfoDateSeparatorFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelInfoFileSubtitleFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelInfoVoiceSubtitleFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelLastMessageSenderNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultChannelSubtitleFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultDraftMessageBodyFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMediaDurationFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMentionUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageBodyFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageBodyWithAttachmentsFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageDateSeparatorFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageInfoDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultMessageViewCountFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUnreadCountFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserNameFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserPresenceDateFormatter
import com.sceyt.chatuikit.formatters.defaults.DefaultUserShortNameFormatter
import com.sceyt.chatuikit.persistence.lazyVar
import java.util.Date

class SceytChatUIKitFormatters {

    var userPresenceDateFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserPresenceDateFormatter()
    }

    var userNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserNameFormatter()
    }

    var userShortNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserShortNameFormatter()
    }

    var mentionUserNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultMentionUserNameFormatter()
    }

    var typingUserNameFormatter: Formatter<SceytUser> by lazyVar {
        DefaultUserShortNameFormatter()
    }

    var reactedUserNameFormatter: Formatter<SceytUser> by lazyVar {
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

    var channelLastMessageSenderNameFormatter: Formatter<SceytChannel> by lazyVar {
        DefaultChannelLastMessageSenderNameFormatter()
    }

    var messageBodyFormatter: Formatter<MessageBodyFormatterAttributes> by lazyVar {
        DefaultMessageBodyFormatter()
    }

    var channelLastMessageBodyFormatter: Formatter<MessageBodyFormatterAttributes> by lazyVar {
        DefaultMessageBodyWithAttachmentsFormatter()
    }

    var editMessageBodyFormatter: Formatter<MessageBodyFormatterAttributes> by lazyVar {
        DefaultMessageBodyWithAttachmentsFormatter()
    }

    var replyMessageBodyFormatter: Formatter<MessageBodyFormatterAttributes> by lazyVar {
        DefaultMessageBodyWithAttachmentsFormatter()
    }

    var repliedMessageBodyFormatter: Formatter<MessageBodyFormatterAttributes> by lazyVar {
        DefaultMessageBodyWithAttachmentsFormatter()
    }

    var draftMessageBodyFormatter: Formatter<DraftMessageBodyFormatterAttributes> by lazyVar {
        DefaultDraftMessageBodyFormatter()
    }

    var messageDateFormatter: Formatter<Date> by lazyVar {
        DefaultMessageDateFormatter()
    }

    var messageInfoDateFormatter: Formatter<Date> by lazyVar {
        DefaultMessageInfoDateFormatter()
    }

    var messageDateSeparatorFormatter: Formatter<Date> by lazyVar {
        DefaultMessageDateSeparatorFormatter()
    }

    var channelInfoAttachmentDateFormatter: Formatter<Date> by lazyVar {
        DefaultChannelInfoAttachmentDateFormatter()
    }

    var channelInfoDateSeparatorFormatter: Formatter<Date> by lazyVar {
        DefaultChannelInfoDateSeparatorFormatter()
    }

    var channelInfoFileSubtitleFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultChannelInfoFileSubtitleFormatter()
    }

    var channelInfoVoiceSubtitleFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultChannelInfoVoiceSubtitleFormatter()
    }

    var messageViewCountFormatter: Formatter<Long> by lazyVar {
        DefaultMessageViewCountFormatter()
    }

    var attachmentNameFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultAttachmentNameFormatter()
    }

    var attachmentSizeFormatter: Formatter<SceytAttachment> by lazyVar {
        DefaultAttachmentSizeFormatter()
    }

    var mediaDurationFormatter: Formatter<Long> by lazyVar {
        DefaultMediaDurationFormatter()
    }

    var unreadCountFormatter: Formatter<Long> by lazyVar {
        DefaultUnreadCountFormatter()
    }
}