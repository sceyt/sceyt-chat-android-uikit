package com.sceyt.sceytchatuikit.data.models.channels

import com.sceyt.chat.models.member.Member

data class CreateChannelData(
        var channelType: ChannelTypeEnum = ChannelTypeEnum.Public,
        var uri: String = "",
        var subject: String = "",
        var avatarUrl: String = "",
        var metadata: String = "",
        var members: List<Member> = arrayListOf()
) {
    var avatarUploaded: Boolean = false
}
