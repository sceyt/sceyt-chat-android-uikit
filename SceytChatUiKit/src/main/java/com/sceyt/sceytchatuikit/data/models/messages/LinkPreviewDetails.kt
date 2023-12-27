package com.sceyt.sceytchatuikit.data.models.messages

import com.vanniktech.ui.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LinkPreviewDetails(
        val link: String,
        val url: String?,
        val description: String?,
        var siteName: String?,
        var faviconUrl: String?,
        val imageUrl: String?,
        val imageWidth: Int?,
        val imageHeight: Int?,
) : Parcelable