package com.sceyt.chatuikit.persistence.filetransfer

import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.customviews.CircularProgressView

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
    WaitingToUpload,

    //This state is not saving to db.
    FilePathChanged,
    ThumbLoaded
}

fun CircularProgressView.getProgressWithState(state: TransferState, progressPercent: Float = 0f) {
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

        Downloading, Uploading, FilePathChanged, Preparing -> {
            setProgress(progressPercent)
            setIcon(context.getCompatDrawable(R.drawable.sceyt_ic_cancel_transfer))
            isVisible = true
        }

        WaitingToUpload -> {
            setProgress(0f)
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