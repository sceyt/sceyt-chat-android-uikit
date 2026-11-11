package com.sceyt.chatuikit.filetransfer.encryption

import com.sceyt.chat.fileencryption.RandomAccessStream
import java.io.File
import java.io.InterruptedIOException
import java.io.RandomAccessFile

/** The SDK reads encrypted ranges from the completed local download. */
internal class FileRandomAccessStream(file: File) : RandomAccessStream {
    private val input = RandomAccessFile(file, "r")
    private val size = input.length()
    val sourceId = file.absolutePath + ":" + size + ":" + file.lastModified()
    private var closed = false

    override fun size(): Long = size

    @Synchronized
    override fun read(offset: Long, length: Int): ByteArray {
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
