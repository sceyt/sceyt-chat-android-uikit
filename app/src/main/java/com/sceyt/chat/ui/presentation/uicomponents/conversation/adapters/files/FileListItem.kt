package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import androidx.databinding.BaseObservable
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import java.io.File
import kotlin.math.max

sealed class FileListItem(val file: Attachment?,
                          val sceytUiMessage: SceytUiMessage) : BaseObservable() {
    data class File(val attachment: Attachment?,
                    val message: SceytUiMessage) : FileListItem(attachment, message)

    data class Image(val attachment: Attachment?,
                     val message: SceytUiMessage) : FileListItem(attachment, message)

    data class Video(val attachment: Attachment?,
                     val message: SceytUiMessage) : FileListItem(attachment, message)


    fun setUploadListener(finishCb: ((success: Boolean) -> Unit)? = null) {
        fileLoadData.loading = true
        file?.setUploaderProgress(object : ProgressCallback {
            override fun onResult(progress: Float) {
                val intValuePercent = (progress * 100).toInt()
                if (intValuePercent < 100)
                    fileLoadData.update(max(1, intValuePercent), true)
            }

            override fun onError(p0: SceytException?) {
                fileLoadData.update(null, false)
                sceytUiMessage.status = DeliveryStatus.Failed
                println("Upload error ->$p0")
            }
        })

        file?.setUploaderCompletion(object : ActionCallback {
            override fun onSuccess() {
                fileLoadData.update(100, false)
                finishCb?.invoke(true)
            }

            override fun onError(p0: SceytException?) {
                fileLoadData.update(null, false)
                sceytUiMessage.status = DeliveryStatus.Failed
                finishCb?.invoke(false)
                println("Upload error ->$p0")
            }
        })
    }

    val updateDownloadState = { progress: Int?, loading: Boolean ->
        val progressValue = if (progress != null) max(1, progress) else null
        fileLoadData.update(progressValue, loading)
    }

    var downloadSuccess: ((java.io.File) -> Unit)? = null
    val fileLoadData by lazy { FileLoadData() }
}

