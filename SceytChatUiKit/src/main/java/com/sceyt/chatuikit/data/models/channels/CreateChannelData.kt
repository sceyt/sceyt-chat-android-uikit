package com.sceyt.chatuikit.data.models.channels

data class CreateChannelData(
        var type: String,
        var uri: String = "",
        var subject: String = "",
        var avatarUrl: String = "",
        var metadata: String = "",
        var members: List<SceytMember> = emptyList(),
) {
    var avatarUploaded: Boolean = false
}
