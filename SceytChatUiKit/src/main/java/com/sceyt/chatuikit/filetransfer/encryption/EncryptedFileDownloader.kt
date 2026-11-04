package com.sceyt.chatuikit.filetransfer.encryption

import com.google.gson.Gson
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import com.sceyt.chatuikit.logger.SceytLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

internal data class FileDownloadEncryption(
    val info: FileEncryptionInfo,
    val plaintextSize: Long,
)

/** Downloads ciphertext, then publishes verified plaintext. */
internal class EncryptedFileDownloader(directory: File) {
    private val gson = Gson()
    private val sessions = EncryptedDownloadSessionStore(directory)

    suspend fun download(
        request: FileDownloadRequest,
        encryptionInfo: FileDownloadEncryption,
        encryption: FileEncryption,
        callback: FileTransferCallback,
        downloadCiphertext: suspend (FileDownloadRequest, FileTransferCallback) -> FileTransferResult<String>,
    ): FileTransferResult<String> = withContext(Dispatchers.IO) {
        val destination = request.destinationFile
        val encrypted = File(destination.path + ".sceyt.enc")
        val partial = File(encrypted.path + ".part")
        try {
            var saved = sessions.get(request.operationId)
            if (saved != null && (saved.discardRequested || saved.destination != destination.absolutePath)) {
                sessions.discard(request.operationId)
                saved = null
            }
            // Persist the destination before any network writer starts, including for cancel after restart.
            sessions.save(
                request.operationId,
                EncryptedDownloadSession(
                    source = saved?.source.orEmpty(),
                    destination = destination.absolutePath,
                    plaintextOffset = saved?.plaintextOffset ?: 0,
                ),
            )
            if (!encrypted.isFile) {
                val result =
                    downloadCiphertext(request.copy(destinationFile = partial)) {
                        callback.onProgress(it * 0.9f)
                    }
                if (result !is FileTransferResult.Success) {
                    if (result is FileTransferResult.Failure) discard(request.operationId)
                    return@withContext result
                }
                check(partial.renameTo(encrypted)) { "Cannot finalize the encrypted download" }
            }
            callback.onProgress(90f)
            val result = decrypt(
                operationId = request.operationId,
                destination = destination,
                encryptedFile = encrypted,
                encryption = encryption,
                encryptionInfo = encryptionInfo,
                callback = { callback.onProgress(90f + it * 0.1f) },
            )
            encrypted.delete()
            callback.onProgress(100f)
            FileTransferResult.Success(result)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            discard(request.operationId)
            FileTransferResult.Failure(error)
        }
    }

    private suspend fun decrypt(
        operationId: String,
        destination: File,
        encryptedFile: File,
        encryption: FileEncryption,
        encryptionInfo: FileDownloadEncryption,
        callback: FileTransferCallback,
    ): String {
        val partial = File(destination.path + ".sceyt.part")
        val plaintext = encryption.prepareDecryption(
            encryptedFile,
            encryptionInfo.info,
            encryptionInfo.plaintextSize
        )
        return try {
            check(plaintext.plaintextSize == encryptionInfo.plaintextSize) { "Unexpected decrypted file size" }
            val identity = EncryptedDownloadSession(
                source = hash(plaintext.sourceId + "\u0000" + gson.toJson(encryptionInfo)),
                destination = destination.absolutePath,
            )
            val saved = sessions.get(operationId)
            val resume =
                saved?.source == identity.source && saved.destination == identity.destination &&
                        saved.plaintextOffset in 0..plaintext.plaintextSize &&
                        partial.isFile && partial.length() >= saved.plaintextOffset
            check(destination.parentFile?.let { it.isDirectory || it.mkdirs() } != false) {
                "Cannot create download directory"
            }
            RandomAccessFile(partial, "rw").use { output ->
                var offset = if (resume) saved.plaintextOffset else 0
                // Discard any tail written before a crash but not committed to the checkpoint.
                output.setLength(offset)
                output.fd.sync()
                sessions.save(operationId, identity.copy(plaintextOffset = offset))
                output.seek(offset)
                while (offset < plaintext.plaintextSize) {
                    currentCoroutineContext().ensureActive()
                    val length =
                        minOf(BUFFER_SIZE.toLong(), plaintext.plaintextSize - offset).toInt()
                    val bytes = plaintext.read(offset, length)
                    check(bytes.size == length) { "Decryption returned an incomplete plaintext range" }
                    output.write(bytes)
                    output.fd.sync()
                    offset += bytes.size
                    sessions.save(operationId, identity.copy(plaintextOffset = offset))
                    callback.onProgress(offset * 100f / plaintext.plaintextSize)
                }
            }
            currentCoroutineContext().ensureActive()
            check(partial.renameTo(destination)) { "Cannot publish the decrypted attachment" }
            sessions.delete(operationId)
            destination.path
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { plaintext.close() }
        }
    }

    /** Call after the download coroutine has stopped, so no writer owns the temporary files. */
    fun discard(operationId: String) {
        runCatching { sessions.discard(operationId) }.onFailure {
            SceytLog.e("EncryptedFileDownloader", "Download cleanup remains pending", it)
        }
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val BUFFER_SIZE = 1024 * 1024
    }
}
