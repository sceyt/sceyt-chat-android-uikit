package com.sceyt.sceytchatuikit.persistence.filetransfer

import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
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
    Preparing,

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
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }

        Preparing -> {
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }

        Uploaded, Downloaded -> isVisible = false

        ThumbLoaded -> {
            if (progressPercent == 100f)
                isVisible = false
        }
    }
}