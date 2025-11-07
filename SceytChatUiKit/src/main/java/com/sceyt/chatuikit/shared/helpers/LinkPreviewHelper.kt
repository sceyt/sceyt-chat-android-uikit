package com.sceyt.chatuikit.shared.helpers

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.extensions.getImageBitmapWithGlideWithTimeout
import com.sceyt.chatuikit.extensions.toBase64
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.shared.utils.BitmapUtil
import com.sceyt.chatuikit.shared.utils.FileResizeUtil
import com.sceyt.chatuikit.shared.utils.ThumbHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class LinkPreviewHelper : SceytKoinComponent {
    private val attachmentLogic: PersistenceAttachmentLogic by inject()
    private var scope: CoroutineScope
    private val context: Context

    constructor(context: Context) {
        this.context = context
        scope = (context as? AppCompatActivity)?.lifecycleScope ?: initDefaultScope()
    }

    constructor(context: Context, scope: CoroutineScope) {
        this.context = context
        this.scope = scope
    }

    private fun initDefaultScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getPreview(
        attachment: SceytAttachment,
        requireFullData: Boolean,
        successListener: PreviewCallback.Success? = null,
        errorListener: PreviewCallback.Error? = null
    ) {

        scope.launch(Dispatchers.IO) {
            val link = attachment.url ?: return@launch withContext(Dispatchers.Main) {
                errorListener?.error(null)
                return@withContext
            }
            when (val response = attachmentLogic.getLinkPreviewData(link)) {
                is SceytResponse.Error -> errorListener?.error(response.message)
                is SceytResponse.Success -> {
                    var details = response.data ?: run {
                        errorListener?.error(null)
                        return@launch
                    }
                    if (requireFullData && details.imageUrl != null && details.imageWidth == null) {
                        val bitmap = getImageBitmapWithGlideWithTimeout(context, details.imageUrl)
                        if (bitmap != null) {
                            details = details.copy(
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height
                            )
                            // update link image size
                            attachmentLogic.updateLinkDetailsSize(
                                link = link,
                                size = Size(bitmap.width, bitmap.height)
                            )
                            val thumb = getImageThumb(bitmap)
                            thumb?.let {
                                details = details.copy(thumb = it)
                                // update link thumb
                                attachmentLogic.updateLinkDetailsThumb(link, it)
                            }
                            withContext(Dispatchers.Main) {
                                successListener?.success(details)
                            }
                        } else withContext(Dispatchers.Main) {
                            successListener?.success(details)
                        }
                    } else withContext(Dispatchers.Main) {
                        successListener?.success(response.data)
                    }
                }
            }
        }
    }

    fun checkMissedData(
        details: LinkPreviewDetails,
        successListener: PreviewCallback.Success? = null
    ) {
        if (details.imageUrl != null && details.imageWidth == null) {
            scope.launch(Dispatchers.IO) {
                val bitmap = getImageBitmapWithGlideWithTimeout(context, details.imageUrl)
                if (bitmap != null) {
                    var detailsToUpdate = details.copy(
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height
                    )
                    // update link image size
                    attachmentLogic.updateLinkDetailsSize(
                        link = details.link,
                        size = Size(bitmap.width, bitmap.height)
                    )
                    val thumb = getImageThumb(bitmap)
                    thumb?.let {
                        detailsToUpdate = detailsToUpdate.copy(thumb = it)
                        // update link thumb
                        attachmentLogic.updateLinkDetailsThumb(details.link, it)
                    }

                    withContext(Dispatchers.Main) {
                        successListener?.success(detailsToUpdate)
                    }
                }
            }
        }
    }

    private fun getImageThumb(bitmap: Bitmap): String? {
        FileResizeUtil.resizeAndCompressImageAsByteArray(bitmap, 100)?.let { bm ->
            val bytes = ThumbHash.rgbaToThumbHash(bm.width, bm.height, BitmapUtil.bitmapToRgba(bm))
            return bytes.toBase64()
        }
        return null
    }

    /** Be careful, after closing scope, you can't launch any other coroutines.*/
    fun close() {
        scope.cancel()
    }

    sealed interface PreviewCallback {
        fun interface Success : PreviewCallback {
            fun success(linkDetails: LinkPreviewDetails)
        }

        fun interface Error : PreviewCallback {
            fun error(message: String?)
        }
    }
}