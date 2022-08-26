package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member

data class CreateChannelData(
        var channelType: Channel.Type = Channel.Type.Public,
        var uri: String? = null,
        var subject: String? = null,
        var avatarUrl: String? = null,
        var label: String? = null,
        var metadata: String? = null,
        var members: List<Member> = arrayListOf()
) {
    var avatarUploaded: Boolean = false
}
