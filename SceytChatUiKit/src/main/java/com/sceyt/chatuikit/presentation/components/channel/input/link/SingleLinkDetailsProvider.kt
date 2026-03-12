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
        val link = text.extractLinks().firstOrNull { it.isValidUrl(context) }
        if (link == null) {
            detailsCallback(null)
            return
        }
        if (loadedLinks.containsKey(link)) {
            detailsCallback(loadedLinks[link])
            return
        }
        loadDetailsJob = scope.launch {

            val response = attachmentsMiddleWare.getLinkPreviewData(link)
            if (response is SceytResponse.Success && response.data != null) {
                var linkPreviewDetails = response.data
                loadedLinks[link] = linkPreviewDetails
                withContext(Dispatchers.Main) {
                    detailsCallback(linkPreviewDetails)
                }

                if (linkPreviewDetails.imageUrl != null && linkPreviewDetails.imageWidth == null) {
                    val bitmap = getImageBitmapWithGlideWithTimeout(
                        context = context,
                        url = linkPreviewDetails.imageUrl
                    )

                    if (bitmap == null) return@launch

                    val size = Size(bitmap.width, bitmap.height)
                    linkPreviewDetails = linkPreviewDetails.copy(
                        imageWidth = size.width,
                        imageHeight = size.height
                    )
                    withContext(Dispatchers.Main) {
                        imageSizeCallback(size)
                    }
                    val thumb =
                        if (linkPreviewDetails.thumb == null) getImageThumb(bitmap) else null
                    if (thumb != null) {
                        linkPreviewDetails = linkPreviewDetails.copy(thumb = thumb)
                        withContext(Dispatchers.Main) {
                            thumbCallback(thumb)
                        }
                    }
                    attachmentsMiddleWare.updateLinkDetails(link = link, size = size, thumb = thumb)
                    loadedLinks[link] = linkPreviewDetails
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
        loadedLinks[link]?.let { return it }

        val response = attachmentsMiddleWare.getLinkPreviewData(link)
        if (response !is SceytResponse.Success || response.data == null) return null

        var linkPreviewDetails = response.data
        loadedLinks[link] = linkPreviewDetails

        if (linkPreviewDetails.imageUrl != null && linkPreviewDetails.imageWidth == null) {
            val bitmap = getImageBitmapWithGlideWithTimeout(
                context = context,
                url = linkPreviewDetails.imageUrl
            ) ?: return linkPreviewDetails

            val size = Size(bitmap.width, bitmap.height)
            linkPreviewDetails =
                linkPreviewDetails.copy(imageWidth = size.width, imageHeight = size.height)
            val thumb = if (linkPreviewDetails.thumb == null) getImageThumb(bitmap) else null
            if (thumb != null)
                linkPreviewDetails = linkPreviewDetails.copy(thumb = thumb)
            attachmentsMiddleWare.updateLinkDetails(link = link, size = size, thumb = thumb)
            loadedLinks[link] = linkPreviewDetails
        }

        return linkPreviewDetails
    }

    fun cancel() {
        loadDetailsJob?.cancel()
    }
}