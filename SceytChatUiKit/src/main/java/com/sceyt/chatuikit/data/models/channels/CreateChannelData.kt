package com.sceyt.chatuikit.data.models.channels

data class CreateChannelData(
        var channelType: String,
        var uri: String = "",
        var subject: String = "",
        var avatarUrl: String = "",
        var metadata: String = "",
        var members: List<SceytMember> = arrayListOf()
) {
    var avatarUploaded: Boolean = false
}
