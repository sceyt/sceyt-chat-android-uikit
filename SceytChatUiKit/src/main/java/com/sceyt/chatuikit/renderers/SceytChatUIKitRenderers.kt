package com.sceyt.chatuikit.renderers

import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.renderers.defaults.DefaultChannelAvatarRenderer
import com.sceyt.chatuikit.renderers.defaults.DefaultUserAvatarRenderer

class SceytChatUIKitRenderers {
    var channelAvatarRenderer: ChannelAvatarRenderer by lazyVar {
        DefaultChannelAvatarRenderer()
    }

    var userAvatarRenderer: UserAvatarRenderer by lazyVar {
        DefaultUserAvatarRenderer()
    }
}