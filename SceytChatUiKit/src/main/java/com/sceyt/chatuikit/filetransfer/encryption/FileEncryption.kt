package com.sceyt.chatuikit.filetransfer.encryption

import java.io.File

/** Cryptography used by transports; implementations own neither networking nor persistence. */
interface FileEncryption {
    suspend fun prepare(file: File): FileEncryptionSession

    /** Restores the same key and encrypted bytes from a privately stored checkpoint. */
    suspend fun restore(file: File, checkpoint: FileEncryptionCheckpoint): FileEncryptionSession
}
