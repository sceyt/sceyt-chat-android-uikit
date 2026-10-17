package com.sceyt.chatuikit.data.models.channels

data class EditChannelData(
        val newSubject: String?,
        val metadata: String?,
        val avatar: ChannelAvatar?,
        val channelUri: String?,
        val channelType: String,
)
