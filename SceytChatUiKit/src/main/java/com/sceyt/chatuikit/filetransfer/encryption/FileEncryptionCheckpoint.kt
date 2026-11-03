package com.sceyt.chatuikit.filetransfer.encryption

/** Private recovery state for one encryption session. Contains key material; do not log it. */
data class FileEncryptionCheckpoint(
    val encryptionInfo: FileEncryptionInfo,
    val plaintextSize: Long,
    val data: String,
) {
    override fun toString(): String =
        "FileEncryptionCheckpoint(encryptionInfo=$encryptionInfo, plaintextSize=$plaintextSize)"
}
