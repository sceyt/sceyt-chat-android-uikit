package com.sceyt.chatuikit.providers

import android.content.Context
import android.graphics.drawable.Drawable
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.presentation.customviews.AvatarView
import com.sceyt.chatuikit.providers.defaults.DefaultAttachmentIconProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelDefaultAvatarProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelListAttachmentIconProvider
import com.sceyt.chatuikit.providers.defaults.DefaultChannelURIValidationMessageProvider
import com.sceyt.chatuikit.providers.defaults.DefaultUserAvatarProvider
import com.sceyt.chatuikit.providers.defaults.URIValidationType

class SceytChatUIKitProviders(
        private val context: Context
) {
    var attachmentIconProvider: VisualProvider<SceytAttachment, Drawable?> by lazyVar {
        DefaultAttachmentIconProvider(context)
    }

    var channelListAttachmentIconProvider: VisualProvider<SceytAttachment, Drawable?> by lazyVar {
        DefaultChannelListAttachmentIconProvider(context)
    }

    var channelDefaultAvatarProvider: VisualProvider<SceytChannel, AvatarView.DefaultAvatar> by lazyVar {
        DefaultChannelDefaultAvatarProvider()
    }

    var userDefaultAvatarProvider: VisualProvider<User, AvatarView.DefaultAvatar> by lazyVar {
        DefaultUserAvatarProvider()
    }

    var channelURIValidationMessageProvider: VisualProvider<URIValidationType, String> by lazyVar {
        DefaultChannelURIValidationMessageProvider(context)
    }
}