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
    Downloaded,
    PauseUpload,
    PauseDownload,
    ErrorUpload,
    ErrorDownload,

    //This state is not saving to db.
    FilePathChanged,
    ThumbLoaded
}

fun SceytCircularProgressView.getProgressWithState(state: TransferState, progressPercent: Float = 0f) {
    when (state) {
        PendingUpload, ErrorUpload, PauseUpload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_upload))
            isVisible = true
        }
        PendingDownload, ErrorDownload, PauseDownload -> {
            release()
            setTransferring(false)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_download))
            isVisible = true
        }
        Downloading, Uploading, FilePathChanged -> {
            setTransferring(true)
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }
        Uploaded, Downloaded -> {
            isVisible = false
        }
        ThumbLoaded -> {
            isVisible = false
        }
    }
}