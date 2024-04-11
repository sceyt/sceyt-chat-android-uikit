package com.sceyt.chatuikit.presentation.uicomponents.messageinput.link

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.di.SceytKoinComponent
import com.sceyt.chatuikit.extensions.extractLinks
import com.sceyt.chatuikit.extensions.getImageBitmapWithGlideWithTimeout
import com.sceyt.chatuikit.extensions.isValidUrl
import com.sceyt.chatuikit.extensions.toBase64
import com.sceyt.chatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.shared.utils.BitmapUtil
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import com.sceyt.chatuikit.shared.utils.ThumbHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class SingleLinkDetailsProvider : SceytKoinComponent {
    private val context: Context
    private val attachmentsMiddleWare: PersistenceAttachmentLogic by inject()
    private var scope: CoroutineScope
    private var loadDetailsJob: Job? = null
    private var loadedLinks = mutableMapOf<String, LinkPreviewDetails>()

    constructor(context: Context) {
        this.context = context
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    constructor(context: Context, scope: CoroutineScope) {
        this.context = context
        this.scope = scope
    }

    fun loadLinkDetails(text: String,
                        detailsCallback: (LinkPreviewDetails?) -> Unit,
                        imageSizeCallback: (Size) -> Unit,
                        thumbCallback: (String) -> Unit) {
        loadDetailsJob?.cancel()
        if (loadedLinks.containsKey(text)) {
            detailsCallback(loadedLinks[text])
            return
        }
        loadDetailsJob = scope.launch(Dispatchers.IO) {
            val link = text.extractLinks().firstOrNull { it.isValidUrl(context) }
            if (link == null) {
                withContext(Dispatchers.Main) { detailsCallback(null) }
                return@launch
            }

            val response = attachmentsMiddleWare.getLinkPreviewData(link)
            if (response is SceytResponse.Success && response.data != null) {
                val linkPreviewDetails = response.data
                loadedLinks[link] = linkPreviewDetails
                withContext(Dispatchers.Main) {
                    detailsCallback(linkPreviewDetails)
                }

                if (linkPreviewDetails.imageUrl != null && linkPreviewDetails.imageWidth == null) {
                    val bitmap = getImageBitmapWithGlideWithTimeout(context, linkPreviewDetails.imageUrl)

                    if (bitmap == null) {
                        withContext(Dispatchers.Main) { detailsCallback(linkPreviewDetails) }
                        return@launch
                    }

                    linkPreviewDetails.imageWidth = bitmap.width
                    linkPreviewDetails.imageHeight = bitmap.height
                    withContext(Dispatchers.Main) {
                        imageSizeCallback(Size(bitmap.width, bitmap.height))
                    }
                    attachmentsMiddleWare.updateLinkDetailsSize(link, Size(bitmap.width, bitmap.height))
                    if (linkPreviewDetails.thumb == null) {
                        val thumb = getImageThumb(bitmap)
                        thumb?.let {
                            linkPreviewDetails.thumb = it
                            withContext(Dispatchers.Main) {
                                thumbCallback.invoke(it)
                            }
                            attachmentsMiddleWare.updateLinkDetailsThumb(link, it)
                        }
                    }
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

    fun cancel() {
        loadDetailsJob?.cancel()
    }
}