package com.sceyt.chatuikit.filetransfer.encryption

/** Private recovery state for one encryption session. Contains key material; do not log it. */
class FileEncryptionCheckpoint(
    val attachmentKey: String,
    val header: ByteArray,
    val data: String,
)
