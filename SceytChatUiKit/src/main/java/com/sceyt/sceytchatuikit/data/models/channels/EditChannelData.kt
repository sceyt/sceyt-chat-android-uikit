package com.sceyt.sceytchatuikit.data.models.channels

data class EditChannelData(
        val newSubject: String?,
        val metadata: String?,
        var avatarUrl: String?,
        val channelUri: String?,
        val channelType: String,
        val avatarEdited: Boolean
)
