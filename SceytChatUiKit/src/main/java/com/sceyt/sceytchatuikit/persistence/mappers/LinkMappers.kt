package com.sceyt.sceytchatuikit.persistence.mappers

import com.sceyt.chat.models.link.LinkDetails
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.persistence.entity.link.LinkDetailsEntity

fun LinkDetails.toLinkDetailsEntity(link: String, thumb: String?): LinkDetailsEntity {
    val image = images.getOrNull(0)
    return LinkDetailsEntity(
        link = link,
        url = url,
        title = title,
        description = description,
        siteName = site_name,
        faviconUrl = favicon?.url,
        imageUrl = image?.url,
        imageWidth = image?.width?.toIntOrNull(),
        imageHeight = image?.height?.toIntOrNull(),
        thumb = thumb
    )
}

fun LinkPreviewDetails.toLinkDetailsEntity() = LinkDetailsEntity(
    link = link,
    url = url,
    title = title,
    description = description,
    siteName = siteName,
    faviconUrl = faviconUrl,
    imageUrl = imageUrl,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    thumb = thumb
)

fun LinkDetails.toLinkPreviewDetails(link: String): LinkPreviewDetails {
    val image = images.getOrNull(0)
    return LinkPreviewDetails(
        link = link,
        url = url,
        title = title,
        description = description,
        siteName = site_name,
        faviconUrl = favicon?.url,
        imageUrl = image?.url,
        imageWidth = image?.width?.toIntOrNull(),
        imageHeight = image?.height?.toIntOrNull(),
        thumb = null,
        hideDetails = false
    )
}

fun LinkDetailsEntity.toLinkPreviewDetails(hideDetails: Boolean): LinkPreviewDetails = LinkPreviewDetails(
    link = link,
    url = url,
    title = title,
    description = description,
    siteName = siteName,
    faviconUrl = faviconUrl,
    imageUrl = imageUrl,
    imageWidth = imageWidth,
    imageHeight = imageHeight,
    thumb = thumb,
    hideDetails = hideDetails
)