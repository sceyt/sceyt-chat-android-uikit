package com.sceyt.sceytchatuikit.persistence.filetransfer

interface FileTransferService : FileTransferListeners.Listeners {
    fun setCustomListener(fileTransferListeners: FileTransferListeners.Listeners)
}