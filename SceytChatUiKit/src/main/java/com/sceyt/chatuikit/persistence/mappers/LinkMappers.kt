package com.sceyt.chatuikit.persistence.mappers

import com.sceyt.chat.models.link.LinkDetails
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity

internal fun LinkPreviewDetails.toLinkDetailsEntity() = LinkDetailsEntity(
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
    val image = images.firstOrNull()
    return LinkPreviewDetails(
        link = link,
        url = url,
        title = title.take(100),
        description = description.take(200),
        siteName = site_name,
        faviconUrl = favicon?.url,
        imageUrl = image?.url,
        imageWidth = image?.width?.toIntOrNull(),
        imageHeight = image?.height?.toIntOrNull(),
        thumb = null,
        hideDetails = false
    )
}

internal fun LinkDetailsEntity.toLinkPreviewDetails(hideDetails: Boolean): LinkPreviewDetails = LinkPreviewDetails(
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

internal fun LinkPreviewDetails.mergeWith(newer: LinkPreviewDetails): LinkPreviewDetails {
    val imageSource = if (newer.imageUrl.isNullOrBlank()) this else newer
    val sameImage = !imageUrl.isNullOrBlank() && imageUrl == imageSource.imageUrl
    val sizeSource = if (sameImage && !hasSize(imageSource.imageWidth, imageSource.imageHeight)) this else imageSource
    return newer.copy(
        url = newer.url.orIfBlank(url),
        title = newer.title.orIfBlank(title),
        description = newer.description.orIfBlank(description),
        siteName = newer.siteName.orIfBlank(siteName),
        faviconUrl = newer.faviconUrl.orIfBlank(faviconUrl),
        imageUrl = imageSource.imageUrl,
        imageWidth = sizeSource.imageWidth,
        imageHeight = sizeSource.imageHeight,
        thumb = if (sameImage) imageSource.thumb.orIfBlank(thumb) else imageSource.thumb
    )
}

internal fun LinkDetailsEntity.mergeWith(newer: LinkDetailsEntity): LinkDetailsEntity {
    val merged = toLinkPreviewDetails(false).mergeWith(newer.toLinkPreviewDetails(false))
    return merged.toLinkDetailsEntity()
}

internal fun LinkDetailsEntity.keepImageDataFrom(stored: LinkDetailsEntity?): LinkDetailsEntity {
    if (stored == null || imageUrl.isNullOrBlank() || imageUrl != stored.imageUrl) return this
    val sizeSource = if (hasSize(imageWidth, imageHeight)) this else stored
    return copy(
        imageWidth = sizeSource.imageWidth,
        imageHeight = sizeSource.imageHeight,
        thumb = thumb.orIfBlank(stored.thumb)
    )
}

private fun hasSize(width: Int?, height: Int?): Boolean = (width ?: 0) > 0 && (height ?: 0) > 0

private fun String?.orIfBlank(fallback: String?): String? = if (isNullOrBlank()) fallback else this
