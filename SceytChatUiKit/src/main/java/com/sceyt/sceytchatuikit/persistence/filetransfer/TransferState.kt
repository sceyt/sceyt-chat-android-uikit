package com.sceyt.sceytchatuikit.persistence.filetransfer

import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView

enum class TransferState {
    PendingUpload,
    Uploading,
    Uploaded,
    PendingDownload,
    Downloading,
    Downloaded,
    ErrorDownload,
    ErrorUpload
}

fun SceytCircularProgressView.getProgressWithState(state: TransferState, progressPercent: Float = 0f) {
    when (state) {
        TransferState.PendingUpload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_upload))
            isVisible = true
        }
        TransferState.PendingDownload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_download))
            isVisible = true

        }
        TransferState.Downloading -> {
            setTransferring(true)
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }
        TransferState.Uploading -> {
            setTransferring(true)
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }
        TransferState.Uploaded -> {
            isVisible = false
        }
        TransferState.Downloaded -> {
            isVisible = false
        }
        TransferState.ErrorDownload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_download))
            isVisible = true
        }
        TransferState.ErrorUpload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_upload))
            isVisible = true
        }
    }
}