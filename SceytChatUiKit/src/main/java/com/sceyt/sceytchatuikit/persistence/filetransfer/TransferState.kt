package com.sceyt.sceytchatuikit.persistence.filetransfer

import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView

enum class TransferState {
    PendingUpload,
    PendingDownload,
    Uploading,
    Downloading,
    Uploaded,
    Downloaded
}

fun SceytCircularProgressView.getProgressWithState(state: TransferState, progressPercent: Float = 0f) {
    when (state) {
        PendingUpload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_upload))
            isVisible = true
        }
        PendingDownload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_download))
            isVisible = true
        }
        Downloading, Uploading -> {
            setTransferring(true)
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }
        Uploaded, Downloaded -> {
            isVisible = false
        }
    }
}