package com.sceyt.chatuikit.data.models.messages

import android.os.Parcelable
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
        val imageWidth: Int?,
        val imageHeight: Int?,
        val thumb: String?,
        val hideDetails: Boolean,
) : Parcelable