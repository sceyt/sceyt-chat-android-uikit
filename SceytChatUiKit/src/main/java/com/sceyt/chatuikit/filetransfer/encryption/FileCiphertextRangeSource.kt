package com.sceyt.chatuikit.filetransfer.encryption

import com.sceyt.chat.fileencryption.CiphertextRangeSource
import java.io.File
import java.io.InterruptedIOException
import java.io.RandomAccessFile

/** The SDK reads encrypted ranges from the completed local download. */
internal class FileCiphertextRangeSource(file: File) : CiphertextRangeSource {
    private val input = RandomAccessFile(file, "r")
    private val size = input.length()
    private val identity = file.absolutePath + ":" + size + ":" + file.lastModified()
    private var closed = false

    override fun getCiphertextSize(): Long = size

    override fun getSourceId(): String = identity

    @Synchronized
    override fun readCiphertextRange(offset: Long, length: Int): ByteArray {
        check(!closed) { "The encrypted file is closed" }
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedIOException("Encrypted file read interrupted")
        }
        require(offset in 0..size && length >= 0 && length.toLong() <= size - offset)
        return ByteArray(length).also {
            input.seek(offset)
            input.readFully(it)
        }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        input.close()
    }
}
