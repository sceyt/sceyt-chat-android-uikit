package com.sceyt.chatuikit.data.models.channels

import com.google.gson.annotations.SerializedName

data class SelfChannelMetadata(
        @SerializedName("s")
        val isSelf: Int?
)
