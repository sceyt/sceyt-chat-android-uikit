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
) : Parcelable {

    val shouldShowImage: Boolean
        get() = !hideDetails && !imageUrl.isNullOrBlank()

    companion object {

        fun hiddenLink(link: String) = LinkPreviewDetails(
            link = link,
            title = null,
            url = null,
            description = null,
            siteName = null,
            faviconUrl = null,
            imageUrl = null,
            imageWidth = null,
            imageHeight = null,
            thumb = null,
            hideDetails = true
        )
    }
}