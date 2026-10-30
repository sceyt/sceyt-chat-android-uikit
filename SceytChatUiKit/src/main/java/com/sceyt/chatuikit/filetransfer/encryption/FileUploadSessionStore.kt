package com.sceyt.chatuikit.filetransfer.encryption

import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

internal class FileUploadSessionStore(private val directory: File) : UploadSessionStore {
    private val gson = Gson()

    override fun get(operationId: String): UploadSession? = synchronized(lock) {
        val atomic = file(operationId)
        val json = try {
            atomic.openRead().bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            return null
        }
        try {
            gson.fromJson(json, UploadSession::class.java)
        } catch (_: JsonParseException) {
            atomic.delete()
            null
        }
    }

    override fun save(session: UploadSession) = synchronized(lock) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create upload session directory" }
        val atomic = file(session.operationId)
        val output = atomic.startWrite()
        try {
            output.write(gson.toJson(session).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
    }

    override fun delete(operationId: String) = synchronized(lock) {
        file(operationId).delete()
    }

    private fun file(operationId: String): AtomicFile {
        val name = MessageDigest.getInstance("SHA-256")
            .digest(operationId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$name.json"))
    }

    private companion object {
        val lock = Any()
    }
}
