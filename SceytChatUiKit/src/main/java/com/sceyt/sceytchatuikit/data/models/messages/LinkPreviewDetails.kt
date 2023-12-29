package com.sceyt.sceytchatuikit.data.models.messages

import com.vanniktech.ui.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LinkPreviewDetails(
        val link: String,
        val title: String?,
        val url: String?,
        val description: String?,
        val siteName: String?,
        val faviconUrl: String?,
        val imageUrl: String?,
        var imageWidth: Int?,
        var imageHeight: Int?,
        var thumb: String?
) : Parcelable