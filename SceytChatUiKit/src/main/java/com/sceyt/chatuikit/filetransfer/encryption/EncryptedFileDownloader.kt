package com.sceyt.chatuikit.filetransfer.encryption

import android.util.AtomicFile
import com.google.gson.Gson
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.FileEncryptionProvider
import com.sceyt.chat.fileencryption.PlaintextReader
import com.sceyt.chatuikit.filetransfer.FileDownloadRequest
import com.sceyt.chatuikit.filetransfer.FileTransferCallback
import com.sceyt.chatuikit.filetransfer.FileTransferResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/** Downloads encrypted bytes, then uses the SDK to publish verified plaintext. */
internal class EncryptedFileDownloader(private val directory: File) {
    private val gson = Gson()

    suspend fun download(
        request: FileDownloadRequest,
        provider: FileEncryptionProvider,
        encryptionInfo: FileEncryptionInfo,
        callback: FileTransferCallback,
        downloadCiphertext: suspend (FileDownloadRequest, FileTransferCallback) -> FileTransferResult<String>,
    ): FileTransferResult<String> = withContext(Dispatchers.IO) {
        val destination = request.destinationFile
        val encrypted = File(destination.path + ".sceyt.enc")
        val partial = File(encrypted.path + ".part")
        try {
            if (!encrypted.isFile) {
                val result = downloadCiphertext(request.copy(destinationFile = partial)) {
                    callback.onProgress(it * 0.9f)
                }
                if (result !is FileTransferResult.Success) {
                    if (result is FileTransferResult.Failure) discard(request.operationId, destination)
                    return@withContext result
                }
                check(partial.renameTo(encrypted)) { "Cannot finalize the encrypted download" }
            }
            callback.onProgress(90f)
            val result = FileCiphertextRangeSource(encrypted).use { source ->
                decrypt(
                    operationId = request.operationId,
                    destination = destination,
                    source = source,
                    provider = provider,
                    encryptionInfo = encryptionInfo,
                    callback = { callback.onProgress(90f + it * 0.1f) },
                )
            }
            encrypted.delete()
            result
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            discard(request.operationId, destination)
            FileTransferResult.Failure(error)
        }
    }

    private suspend fun decrypt(
        operationId: String,
        destination: File,
        source: FileCiphertextRangeSource,
        provider: FileEncryptionProvider,
        encryptionInfo: FileEncryptionInfo,
        callback: FileTransferCallback,
    ): FileTransferResult<String> {
        val partial = File(destination.path + ".sceyt.part")
        val checkpoint = checkpoint(operationId)
        var reader: PlaintextReader? = null
        return try {
            runInterruptible {
                reader = provider.openDownload(source, encryptionInfo)
            }
            val plaintext = checkNotNull(reader)
            val identity = DownloadIdentity(
                source = hash(source.getSourceId() + "\u0000" + gson.toJson(encryptionInfo)),
                destination = destination.absolutePath,
            )
            val resume = readIdentity(checkpoint) == identity &&
                partial.isFile && partial.length() <= plaintext.plaintextSize
            check(destination.parentFile?.let { it.isDirectory || it.mkdirs() } != false) {
                "Cannot create download directory"
            }
            RandomAccessFile(partial, "rw").use { output ->
                if (!resume) {
                    output.setLength(0)
                    output.fd.sync()
                }
                saveIdentity(checkpoint, identity)
                var offset = output.length()
                output.seek(offset)
                while (offset < plaintext.plaintextSize) {
                    currentCoroutineContext().ensureActive()
                    val length = minOf(BUFFER_SIZE.toLong(), plaintext.plaintextSize - offset).toInt()
                    val bytes = runInterruptible { plaintext.read(offset, length) }
                    check(bytes.size == length) { "The SDK returned an incomplete plaintext range" }
                    output.write(bytes)
                    output.fd.sync()
                    offset += bytes.size
                    callback.onProgress(offset * 100f / plaintext.plaintextSize)
                }
            }
            currentCoroutineContext().ensureActive()
            check(partial.renameTo(destination)) { "Cannot publish the decrypted attachment" }
            checkpoint.delete()
            FileTransferResult.Success(destination.path)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            currentCoroutineContext().ensureActive()
            partial.delete()
            checkpoint.delete()
            FileTransferResult.Failure(error)
        } finally {
            reader?.close()
        }
    }

    fun discard(operationId: String, destination: File? = null) {
        val checkpoint = checkpoint(operationId)
        readIdentity(checkpoint)?.let {
            File(it.destination + ".sceyt.part").delete()
        }
        checkpoint.delete()
        destination?.let {
            File(it.path + ".sceyt.enc").delete()
            File(it.path + ".sceyt.enc.part").delete()
        }
    }

    private fun readIdentity(checkpoint: AtomicFile): DownloadIdentity? = runCatching {
        checkpoint.openRead().bufferedReader().use {
            gson.fromJson(it, DownloadIdentity::class.java)
        }
    }.getOrNull()

    private fun saveIdentity(checkpoint: AtomicFile, identity: DownloadIdentity) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create download checkpoint directory" }
        val output = checkpoint.startWrite()
        try {
            output.write(gson.toJson(identity).toByteArray(Charsets.UTF_8))
            checkpoint.finishWrite(output)
        } catch (error: Throwable) {
            checkpoint.failWrite(output)
            throw error
        }
    }

    private fun checkpoint(operationId: String) = AtomicFile(File(directory, hash(operationId) + ".json"))

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private data class DownloadIdentity(val source: String, val destination: String)

    private companion object {
        const val BUFFER_SIZE = 1024 * 1024
    }
}
