package com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention

import com.google.gson.annotations.SerializedName

data class MentionAnnotationValue(
        @SerializedName("userName")
        val userName: String,
        @SerializedName("userId")
        val userId: String
)
