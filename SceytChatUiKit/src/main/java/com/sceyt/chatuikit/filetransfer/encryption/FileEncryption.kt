package com.sceyt.chatuikit.filetransfer.encryption

import java.io.File

/** Cryptography used by file transfer providers; implementations own neither networking nor persistence. */
interface FileEncryption {
    suspend fun prepareEncryption(file: File): FileEncryptionSession

    /** Restores the same key and encrypted bytes from a privately stored checkpoint. */
    suspend fun restoreEncryption(
        file: File,
        checkpoint: FileEncryptionCheckpoint
    ): FileEncryptionSession

    /** Prepares verified plaintext reading from a completed encrypted file. Performs no networking. */
    suspend fun prepareDecryption(
        encryptedFile: File,
        token: String,
    ): FileDecryptionSession
}
