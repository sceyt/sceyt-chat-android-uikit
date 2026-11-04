package com.sceyt.chatuikit.filetransfer.encryption

import android.util.AtomicFile
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

@Keep
internal data class EncryptedDownloadSession(
    val source: String,
    val destination: String,
    val plaintextOffset: Long = 0,
    val discardRequested: Boolean = false,
)

/** Stores only local file identity, never the file key. All access belongs on IO. */
internal class EncryptedDownloadSessionStore(private val directory: File) {
    private val gson = Gson()

    @Synchronized
    fun get(operationId: String): EncryptedDownloadSession? {
        val checkpoint = checkpoint(operationId)
        val text = try {
            checkpoint.openRead().bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            return null
        }
        return try {
            gson.fromJson(text, EncryptedDownloadSession::class.java)
        } catch (_: JsonParseException) {
            checkpoint.delete()
            null
        }
    }

    @Synchronized
    fun save(operationId: String, session: EncryptedDownloadSession) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create download checkpoint directory" }
        val checkpoint = checkpoint(operationId)
        val output = checkpoint.startWrite()
        try {
            output.write(gson.toJson(session).toByteArray(Charsets.UTF_8))
            checkpoint.finishWrite(output)
        } catch (error: Throwable) {
            checkpoint.failWrite(output)
            throw error
        }
    }

    @Synchronized
    fun delete(operationId: String) = checkpoint(operationId).delete()

    /** Leaves published plaintext untouched. Called after all operation writers have closed. */
    @Synchronized
    fun discard(operationId: String) {
        get(operationId)?.let { session ->
            if (!session.discardRequested) save(operationId, session.copy(discardRequested = true))
            val removed = listOf(".sceyt.part", ".sceyt.enc", ".sceyt.enc.part").map { suffix ->
                val file = File(session.destination + suffix)
                !file.exists() || file.delete()
            }
            if (removed.any { !it }) throw IOException("Cannot remove a temporary download file")
        }
        delete(operationId)
    }

    private fun checkpoint(operationId: String): AtomicFile {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(operationId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$hash.json"))
    }
}
