package com.sceyt.chatuikit.filetransfer.encryption

/** Parameters recipients need to decrypt one file. Contains the file key; do not log it. */
data class FileEncryptionInfo(
    val algorithm: String,
    val attachmentId: String,
    val key: String,
) {
    override fun toString(): String =
        "FileEncryptionInfo(algorithm=$algorithm, attachmentId=$attachmentId)"
}
