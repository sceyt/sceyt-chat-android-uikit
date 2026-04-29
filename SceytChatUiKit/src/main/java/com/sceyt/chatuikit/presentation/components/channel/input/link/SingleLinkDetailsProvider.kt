package com.sceyt.chatuikit.presentation.components.channel.input.link

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.extensions.extractLinks
import com.sceyt.chatuikit.extensions.getImageBitmapWithGlideWithTimeout
import com.sceyt.chatuikit.extensions.isValidUrl
import com.sceyt.chatuikit.extensions.toBase64
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.shared.utils.BitmapUtil
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import com.sceyt.chatuikit.shared.utils.ThumbHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class SingleLinkDetailsProvider(
    private val context: Context,
    private val scope: CoroutineScope,
) : SceytKoinComponent {
    private val attachmentsMiddleWare: PersistenceAttachmentLogic by inject()
    private var loadDetailsJob: Job? = null
    private val loadedLinks = mutableMapOf<String, LinkPreviewDetails>()

    fun loadLinkDetails(
        text: String,
        detailsCallback: (LinkPreviewDetails?) -> Unit,
        imageSizeCallback: (Size) -> Unit,
        thumbCallback: (String) -> Unit
    ) {
        loadDetailsJob?.cancel()
        loadDetailsJob = scope.launch {
            val link = withContext(Dispatchers.Default) {
                text.extractLinks().firstOrNull { it.isValidUrl(context) }
            }
            if (link == null) {
                detailsCallback(null)
                return@launch
            }

            val cached = loadedLinks[link]
            if (cached != null) {
                detailsCallback(cached)
                if (!cached.isFullyLoaded()) {
                    val updated = fetchImageDimensionsAndThumb(link, cached)
                    withContext(Dispatchers.Main) {
                        if (updated.imageWidth != null && cached.imageWidth == null)
                            imageSizeCallback(Size(updated.imageWidth, updated.imageHeight!!))
                        if (updated.thumb != null && cached.thumb == null)
                            thumbCallback(updated.thumb)
                    }
                }
                return@launch
            }

            val response = attachmentsMiddleWare.getLinkPreviewData(link)
            if (response is SceytResponse.Success && response.data != null) {
                val linkPreviewDetails = response.data
                loadedLinks[link] = linkPreviewDetails
                withContext(Dispatchers.Main) {
                    detailsCallback(linkPreviewDetails)
                }

                val updated = fetchImageDimensionsAndThumb(link, linkPreviewDetails)
                withContext(Dispatchers.Main) {
                    if (updated.imageWidth != null && linkPreviewDetails.imageWidth == null)
                        imageSizeCallback(Size(updated.imageWidth, updated.imageHeight!!))
                    if (updated.thumb != null && linkPreviewDetails.thumb == null)
                        thumbCallback(updated.thumb)
                }
            } else withContext(Dispatchers.Main) { detailsCallback(null) }
        }
    }

    private fun getImageThumb(bitmap: Bitmap): String? {
        FileResizeUtil.resizeAndCompressImageAsByteArray(bitmap, 100)?.let { bm ->
            val bytes = ThumbHash.rgbaToThumbHash(bm.width, bm.height, BitmapUtil.bitmapToRgba(bm))
            return bytes.toBase64()
        }
        return null
    }

    suspend fun loadLinkDetailsSuspend(text: String): LinkPreviewDetails? {
        val link = text.extractLinks().firstOrNull { it.isValidUrl(context) } ?: return null

        loadedLinks[link]?.let { cached ->
            return if (cached.isFullyLoaded()) cached
            else fetchImageDimensionsAndThumb(link, cached)
        }

        val response = attachmentsMiddleWare.getLinkPreviewData(link)
        if (response !is SceytResponse.Success || response.data == null) return null

        val linkPreviewDetails = response.data
        loadedLinks[link] = linkPreviewDetails

        return fetchImageDimensionsAndThumb(link, linkPreviewDetails)
    }

    private suspend fun fetchImageDimensionsAndThumb(
        link: String,
        details: LinkPreviewDetails,
    ): LinkPreviewDetails {
        if (details.imageUrl == null || details.imageWidth != null) return details
        val bitmap = getImageBitmapWithGlideWithTimeout(
            context = context,
            url = details.imageUrl
        ) ?: return details

        val size = Size(bitmap.width, bitmap.height)
        var updated = details.copy(imageWidth = size.width, imageHeight = size.height)
        val thumb = if (details.thumb == null) getImageThumb(bitmap) else null
        if (thumb != null)
            updated = updated.copy(thumb = thumb)

        attachmentsMiddleWare.updateLinkDetails(link = link, size = size, thumb = thumb)
        loadedLinks[link] = updated
        return updated
    }

    private fun LinkPreviewDetails.isFullyLoaded(): Boolean =
        imageUrl == null || (imageWidth != null && thumb != null)

    fun cancel() {
        loadDetailsJob?.cancel()
    }
}
