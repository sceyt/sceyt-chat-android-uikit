package com.sceyt.sceytchatuikit.persistence.filetransfer

enum class TransferState {
    PendingUpload,
    Uploading,
    Uploaded,
    PendingDownload,
    Downloading,
    Downloaded,
    Error
}