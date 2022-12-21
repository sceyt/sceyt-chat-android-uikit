package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.util.Size
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.decodeByteArrayToBitmap
import org.json.JSONObject

sealed class FileListItem {
    lateinit var file: SceytAttachment
    lateinit var sceytMessage: SceytMessage
    var size: Size? = null
    var thumb: Bitmap? = null

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

        var videoDuration: String? = null
    }

    object LoadingMoreItem : FileListItem()


    private fun String?.getThumbByBytesAndSize() {
        var base64Thumb: ByteArray? = null
        var size: Size? = null
        try {
            val jsonObject = JSONObject(this ?: return)
            val thumbnail = jsonObject.getString("thumbnail")
            base64Thumb = Base64.decode(thumbnail, Base64.NO_WRAP)
            val width = jsonObject.getString("width").toIntOrNull()
            val height = jsonObject.getString("height").toIntOrNull()
            if (width != null && height != null)
                size = Size(width, height)
        } catch (ex: Exception) {
            Log.i(TAG, "Couldn't get data from attachment metadata with reason ${ex.message}")
        }
        if (size == null && base64Thumb == null)
            return

        this@FileListItem.size = size
        this@FileListItem.thumb = base64Thumb?.decodeByteArrayToBitmap()
    }
}

