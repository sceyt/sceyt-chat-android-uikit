package com.sceyt.chatuikit.providers

import android.content.Context
import com.sceyt.chatuikit.persistence.lazyVar

class SceytChatUIKitProviders(
        private val context: Context
) {
    var attachmentIconProvider: AttachmentIconProvider? = null
    var channelDefaultAvatarProvider: ChannelDefaultAvatarProvider? = null
    var userDefaultAvatarProvider: UserDefaultAvatarProvider? = null
    var channelURIValidationMessageProvider: ChannelURIValidationMessageProvider by lazyVar {
        DefaultChannelURIValidationMessageProvider(context)
    }
}