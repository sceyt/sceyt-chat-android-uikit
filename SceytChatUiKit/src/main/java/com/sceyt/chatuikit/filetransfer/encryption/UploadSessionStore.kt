package com.sceyt.chatuikit.filetransfer.encryption

import androidx.annotation.WorkerThread
import com.sceyt.chat.fileencryption.FileEncryptionInfo

/** App-private resume data, including the SDK encryption info and its file key. */
internal data class UploadSession(
    val operationId: String,
    val sourceFilePath: String,
    val originalFilePath: String?,
    val sourceId: String,
    val encryptionInfo: FileEncryptionInfo,
    val sdkCheckpointData: String,
    val transportCheckpointData: String? = null,
)

/** Blocking IO contract, also usable from the SDK's blocking ciphertext reader. */
@WorkerThread
internal interface UploadSessionStore {
    fun get(operationId: String): UploadSession?
    fun save(session: UploadSession)
    fun delete(operationId: String)
}
