package com.sceyt.chatuikit.providers

import android.content.Context
import com.sceyt.chatuikit.persistence.lazyVar

class SceytChatUIKitProviders(
        private val context: Context
) {
    var attachmentIconProvider: AttachmentIconProvider? = null
    var channelDefaultAvatarProvider: ChannelDefaultAvatarProvider by lazyVar {
        DefaultChannelDefaultAvatarProvider()
    }
    var userDefaultAvatarProvider: UserDefaultAvatarProvider by lazyVar {
        DefaultUserAvatarProvider()
    }
    var channelURIValidationMessageProvider: ChannelURIValidationMessageProvider by lazyVar {
        DefaultChannelURIValidationMessageProvider(context)
    }
}