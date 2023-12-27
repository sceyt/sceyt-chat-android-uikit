package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.link.LinkDetails
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.persistence.entity.link.LinkDetailsEntity

fun LinkDetails.toLinkDetailsEntity(link: String, messageTid: Long): LinkDetailsEntity {
    val image = images.getOrNull(0)
    return LinkDetailsEntity(
        link = link,
        messageTid = messageTid,
        url = url,
        description = description,
        siteName = site_name,
        faviconUrl = favicon?.url,
        imageUrl = image?.url,
        imageWidth = image?.width?.toIntOrNull(),
        imageHeight = image?.height?.toIntOrNull()
    )
}

fun LinkDetails.toLinkPreviewDetails(link: String): LinkPreviewDetails {
    val image = images.getOrNull(0)
    return LinkPreviewDetails(
        link = link,
        url = url,
        description = description,
        siteName = site_name,
        faviconUrl = favicon?.url,
        imageUrl = image?.url,
        imageWidth = image?.width?.toIntOrNull(),
        imageHeight = image?.height?.toIntOrNull()
    )
}

fun LinkDetailsEntity.toLinkDetails(): LinkPreviewDetails = LinkPreviewDetails(
    link = link,
    url = url,
    description = description,
    siteName = siteName,
    faviconUrl = faviconUrl,
    imageUrl = imageUrl,
    imageWidth = imageWidth,
    imageHeight = imageHeight
)