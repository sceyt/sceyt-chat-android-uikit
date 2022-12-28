package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.decodeByteArrayToBitmap
import com.sceyt.sceytchatuikit.persistence.constants.SceytConstants
import org.json.JSONObject

sealed class FileListItem {
    lateinit var file: SceytAttachment
    lateinit var sceytMessage: SceytMessage
    var size: Size? = null
    var blurredThumb: Bitmap? = null
    var thumbPath: String? = null

    private constructor()

    constructor(file: SceytAttachment, sceytMessage: SceytMessage) : this() {
        this.file = file
        this.sceytMessage = sceytMessage
        file.metadata.getThumbByBytesAndSize()
    }

    data class File(val attachment: SceytAttachment,
                    val message: SceytMessage) : FileListItem(attachment, message)

    data class Image(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Video(val attachment: SceytAttachment,
                     val message: SceytMessage) : FileListItem(attachment, message) {

        var videoDuration: Long? = null
    }

    object LoadingMoreItem : FileListItem()


    private fun String?.getThumbByBytesAndSize() {
        var base64Thumb: ByteArray? = null
        var size: Size? = null
        var duration: Long? = null
        try {
            val jsonObject = JSONObject(this ?: return)
            jsonObject.getFromJsonObject(SceytConstants.Thumb)?.let {
                base64Thumb = Base64.decode(it, Base64.NO_WRAP)
            }
            val width = jsonObject.getFromJsonObject(SceytConstants.Width)?.toIntOrNull()
            val height = jsonObject.getFromJsonObject(SceytConstants.Height)?.toIntOrNull()
            duration = jsonObject.getFromJsonObject(SceytConstants.Duration)?.toLongOrNull()
            if (width != null && height != null)
                size = Size(width, height)
        } catch (ex: Exception) {
            Log.i(TAG, "Couldn't get data from attachment metadata with reason ${ex.message}")
        }

        this@FileListItem.size = size
        this@FileListItem.blurredThumb = base64Thumb?.decodeByteArrayToBitmap()
        (this@FileListItem as? Video)?.videoDuration = duration
    }

    private fun JSONObject.getFromJsonObject(name: String): String? {
        return try {
            getString(name)
        } catch (ex: Exception) {
            null
        }
    }
}

