package com.sceyt.sceytchatuikit.persistence.filetransfer

enum class ProgressState {
    PendingUpload,
    Uploading,
    Uploaded,
    PendingDownload,
    Downloading,
    Downloaded,
    Error
}