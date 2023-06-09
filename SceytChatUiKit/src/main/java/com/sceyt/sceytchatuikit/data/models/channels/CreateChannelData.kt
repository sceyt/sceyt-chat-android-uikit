package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.member.Member

data class CreateChannelData(
        var channelType: ChannelTypeEnum = ChannelTypeEnum.Public,
        var uri: String? = null,
        var subject: String? = null,
        var avatarUrl: String? = null,
        var metadata: String? = null,
        var members: List<Member> = arrayListOf()
) {
    var avatarUploaded: Boolean = false
}
