package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import androidx.databinding.BaseObservable
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import kotlin.math.max

sealed class FileListItem(val file: Attachment?,
                          val sceytMessage: SceytMessage) : BaseObservable() {
    data class File(val attachment: Attachment?,
                    val message: SceytMessage) : FileListItem(attachment, message)

    data class Image(val attachment: Attachment?,
                     val message: SceytMessage) : FileListItem(attachment, message)

    data class Video(val attachment: Attachment?,
                     val message: SceytMessage) : FileListItem(attachment, message)


    internal fun setUploadListener(loadCb: ((FileLoadData) -> Unit)? = null) {
        loadCb?.invoke(FileLoadData().apply { loading = true })
        file?.setUploaderProgress(object : ProgressCallback {
            override fun onResult(progress: Float) {
                val intValuePercent = (progress * 100).toInt()
                if (intValuePercent < 100) {
                    fileLoadData.update(progress = max(1, intValuePercent), loading = true, success = false)
                    loadCb?.invoke(fileLoadData)
                }
            }

            override fun onError(p0: SceytException?) {
                fileLoadData.update(progress = null, loading = false, success = false)
                loadCb?.invoke(fileLoadData)
            }
        })

        file?.setUploaderCompletion(object : ActionCallback {
            override fun onSuccess() {
                fileLoadData.update(progress = 100, loading = false, success = true)
                loadCb?.invoke(fileLoadData)
            }

            override fun onError(p0: SceytException?) {
                fileLoadData.update(progress = null, loading = false, success = false)
                loadCb?.invoke(fileLoadData)
            }
        })
    }

    internal fun updateDownloadState(progress: Int?, loading: Boolean, success: Boolean) {
        val progressValue = if (progress != null) max(1, progress) else null
        fileLoadData.update(progressValue, loading = loading, success = success)
    }

    val fileLoadData by lazy { FileLoadData() }
}

