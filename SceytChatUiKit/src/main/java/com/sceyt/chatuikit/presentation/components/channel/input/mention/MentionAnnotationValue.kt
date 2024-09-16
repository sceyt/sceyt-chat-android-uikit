package com.sceyt.chatuikit.presentation.components.channel.input.mention

import com.google.gson.annotations.SerializedName

data class MentionAnnotationValue(
        @SerializedName("userName")
        val userName: String,
        @SerializedName("userId")
        val userId: String
)
