package com.sceyt.sceytchatuikit.data.models.channels

data class EditChannelData(
        val newSubject: String?,
        val metadata: String?,
        val label: String?,
        val avatarUrl: String?,
        val channelUrl: String?,
        val channelType: ChannelTypeEnum,
        val avatarEdited: Boolean
)
