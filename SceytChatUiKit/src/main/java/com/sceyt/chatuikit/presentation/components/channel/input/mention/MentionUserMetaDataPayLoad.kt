package com.sceyt.chatuikit.presentation.components.channel.input.mention

import com.google.gson.annotations.SerializedName

data class MentionUserMetaDataPayLoad(
        @SerializedName("id")
        val id: String,
        @SerializedName("loc")
        val loc: Int,
        @SerializedName("len")
        val len: Int
)