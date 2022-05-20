package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files

import androidx.databinding.BaseObservable
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ProgressCallback
import com.sceyt.chat.ui.data.models.messages.UploadData

sealed class FileListItem(file: Attachment?) : BaseObservable() {
    data class File(val file: Attachment?) : FileListItem(file)
    data class Image(val file: Attachment?) : FileListItem(file)
    data class Video(val file: Attachment?) : FileListItem(file)

    init {
        file?.setUploaderProgress(object : ProgressCallback {
            override fun onResult(progress: Float) {
                val intValuePercent = (progress * 100).toInt()
                if (intValuePercent < 100)
                    uploadData.update(intValuePercent, true, null)
            }

            override fun onError(p0: SceytException?) {
                uploadData.update(null, false, p0)
            }
        })

        file?.setUploaderCompletion(object : ActionCallback {
            override fun onSuccess() {
                uploadData.update(100, false, null)
            }

            override fun onError(p0: SceytException?) {
                uploadData.update(null, false, p0)
            }
        })
    }

    val uploadData by lazy { UploadData() }
}