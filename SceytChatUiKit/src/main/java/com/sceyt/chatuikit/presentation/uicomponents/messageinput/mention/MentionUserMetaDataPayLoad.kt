package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention

import com.google.gson.annotations.SerializedName

data class MentionUserMetaDataPayLoad(
        @SerializedName("id")
        val id: String,
        @SerializedName("loc")
        val loc: Int,
        @SerializedName("len")
        val len: Int
)