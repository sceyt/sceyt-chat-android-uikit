package com.sceyt.chatuikit.providers

import android.graphics.drawable.Drawable
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.providers.defaults.DefaultAttachmentIconProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelDefaultAvatarProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelListAttachmentIconProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelURIValidationMessageProvider
import com.sceyt.chatuikit.providers.defaults.DefaultPresenceStateColorProvider
import com.sceyt.chatuikit.providers.defaults.DefaultSenderNameColorProvider
import com.sceyt.chatuikit.providers.defaults.DefaultUserAvatarProvider
import com.sceyt.chatuikit.providers.defaults.URIValidationType

class SceytChatUIKitProviders {
    var attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?> by lazyVar {
        DefaultAttachmentIconProvider
    }

    var channelListAttachmentIconProvider: VisualProvider<SceytAttachment, Drawable?> by lazyVar {
        DefaultChannelListAttachmentIconProvider
    }

    var channelDefaultAvatarProvider: VisualProvider<SceytChannel, AvatarView.DefaultAvatar> by lazyVar {
        DefaultChannelDefaultAvatarProvider
    }

    var channelURIValidationMessageProvider: VisualProvider<URIValidationType, String> by lazyVar {
        DefaultChannelURIValidationMessageProvider
    }

    var userDefaultAvatarProvider: VisualProvider<SceytUser, AvatarView.DefaultAvatar> by lazyVar {
        DefaultUserAvatarProvider
    }

    val senderNameColorProvider: VisualProvider<SceytUser, Int> by lazyVar {
        DefaultSenderNameColorProvider
    }

    var presenceStateColorProvider: VisualProvider<PresenceState, Int> by lazyVar {
        DefaultPresenceStateColorProvider
    }
}