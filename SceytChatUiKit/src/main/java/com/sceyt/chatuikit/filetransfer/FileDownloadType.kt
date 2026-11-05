package com.sceyt.chatuikit.filetransfer

import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionInfo

sealed interface FileDownloadType {
    data object Plain : FileDownloadType

    data class Encrypted(
        val encryptionInfo: FileEncryptionInfo,
        val plaintextSize: Long,
    ) : FileDownloadType
}
