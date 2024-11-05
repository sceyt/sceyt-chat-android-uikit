package com.sceyt.chatuikit.persistence.file_transfer

import androidx.core.view.isVisible
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle

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

fun CircularProgressView.getProgressWithState(
        state: TransferState,
        style: MediaLoaderStyle,
        hideOnThumbLoaded: Boolean,
        progressPercent: Float = 0f,
) {
    when (state) {
        PendingUpload, ErrorUpload, PauseUpload -> {
            release(progressPercent, false)
            setIcon(style.uploadIcon)
            isVisible = true
        }

        PendingDownload, ErrorDownload, PauseDownload -> {
            release(progressPercent, false)
            setIcon(style.downloadIcon)
            isVisible = true
        }

        Downloading, Uploading, FilePathChanged, Preparing -> {
            setProgress(progressPercent)
            setIcon(style.cancelIcon)
            isVisible = true
        }

        WaitingToUpload -> {
            setProgress(0f)
            setIcon(style.cancelIcon)
            isVisible = true
        }

        Uploaded, Downloaded -> isVisible = false

        ThumbLoaded -> {
            if (hideOnThumbLoaded)
                isVisible = false
        }
    }
}