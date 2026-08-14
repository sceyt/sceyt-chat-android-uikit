package com.sceyt.chatuikit.filetransfer

fun interface FileTransferCallback {
    fun onEvent(event: FileTransferEvent)
}

sealed interface FileTransferEvent {
    data class Progress(val progressPercent: Float) : FileTransferEvent

    data object WaitingForNetwork : FileTransferEvent
}
