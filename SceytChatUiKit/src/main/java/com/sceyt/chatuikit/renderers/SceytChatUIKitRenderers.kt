package com.sceyt.chatuikit.renderers

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.renderers.defaults.DefaultChannelAvatarRenderer
import com.sceyt.chatuikit.renderers.defaults.DefaultUserAndNotesAvatarRenderer
import com.sceyt.chatuikit.renderers.defaults.DefaultUserAvatarRenderer
import com.sceyt.chatuikit.renderers.defaults.DefaultVoterAvatarRenderer
import com.sceyt.chatuikit.styles.messages_list.item.VoterAvatarRendererAttributes

class SceytChatUIKitRenderers {
    var channelAvatarRenderer: AvatarRenderer<SceytChannel> by lazyVar {
        DefaultChannelAvatarRenderer()
    }

    var userAvatarRenderer: AvatarRenderer<SceytUser> by lazyVar {
        DefaultUserAvatarRenderer()
    }

    var userAndNotesAvatarRenderer: AvatarRenderer<SceytUser> by lazyVar {
        DefaultUserAndNotesAvatarRenderer()
    }

    var voterAvatarRenderer: AvatarRenderer<VoterAvatarRendererAttributes> by lazyVar {
        DefaultVoterAvatarRenderer()
    }
}