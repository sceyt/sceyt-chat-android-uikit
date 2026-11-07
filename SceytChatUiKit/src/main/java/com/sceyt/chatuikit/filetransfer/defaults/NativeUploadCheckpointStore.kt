package com.sceyt.chatuikit.filetransfer.defaults

import android.util.AtomicFile
import android.util.Base64
import com.google.gson.Gson
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import java.io.File
import java.security.MessageDigest

/** Local header and recovery data. The attachment key stays in attachment metadata. */
internal class NativeUploadCheckpointStore(private val directory: () -> File) {
    private val gson = Gson()

    @Synchronized
    fun read(operationId: String, attachmentKey: String): FileEncryptionCheckpoint? {
        val saved = runCatching {
            file(operationId).openRead().bufferedReader().use {
                gson.fromJson(it, SavedCheckpoint::class.java)
            }
        }.getOrNull() ?: return null
        if (saved.keyHash != hash(attachmentKey)) return null

        return FileEncryptionCheckpoint(attachmentKey, saved.header, saved.data)
    }

    @Synchronized
    fun save(operationId: String, checkpoint: FileEncryptionCheckpoint) {
        val file = file(operationId)
        file.baseFile.parentFile?.mkdirs()
        val output = file.startWrite()
        try {
            val saved = SavedCheckpoint(hash(checkpoint.attachmentKey), checkpoint.header, checkpoint.data)
            output.write(gson.toJson(saved).toByteArray())
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }

    @Synchronized
    fun update(operationId: String, checkpoint: FileEncryptionCheckpoint) {
        // Explicit cancellation may have removed this record while the upload was stopping.
        if (read(operationId, checkpoint.attachmentKey) != null) save(operationId, checkpoint)
    }

    @Synchronized
    fun remove(operationId: String) = file(operationId).delete()

    private fun file(operationId: String): AtomicFile =
        AtomicFile(File(directory(), hash(operationId) + ".json"))

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private class SavedCheckpoint(val keyHash: String, val header: ByteArray, val data: String)
}
