package com.sceyt.chatuikit.filetransfer

/** The outcome of one transport attempt. It does not change persisted transfer states. */
sealed interface FileTransferResult<out T> {
    data class Success<T>(val result: T) : FileTransferResult<T>

    /** A terminal failure. UIKit may discard the partial download. */
    data class Failure(val error: Throwable) : FileTransferResult<Nothing>

    /**
     * The attempt ended before completion, for example because connectivity was lost.
     * The transport must stop reading or writing before returning, but retain any resumable
     * backend state. UIKit preserves the partial download without cancelling the transport.
     * Downloads require a later explicit retry; pending uploads can be resent on reconnect
     * through the existing pending-message flow.
     */
    data class Interrupted(val error: Throwable) : FileTransferResult<Nothing>
}
