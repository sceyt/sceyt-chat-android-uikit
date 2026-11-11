package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.EOFException
import java.io.InterruptedIOException
import java.io.RandomAccessFile

class FileRandomAccessStreamTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `random ranges return exactly the requested bytes including the end of the file`() {
        val bytes = ByteArray(257) { it.toByte() }
        val file = temporary.newFile().apply { writeBytes(bytes) }
        FileRandomAccessStream(file).use { source ->
            assertThat(source.size()).isEqualTo(bytes.size.toLong())
            for ((offset, length) in listOf(200 to 57, 0 to 20, 19 to 100, 257 to 0, 0 to 0)) {
                assertThat(source.read(offset.toLong(), length))
                    .isEqualTo(bytes.copyOfRange(offset, offset + length))
            }
        }
    }

    @Test
    fun `invalid ranges are rejected before allocating or reading`() {
        val file = temporary.newFile().apply { writeBytes(ByteArray(4)) }
        FileRandomAccessStream(file).use { source ->
            val ranges = listOf(-1L to 1, 0L to -1, 5L to 0, 3L to 2, Long.MAX_VALUE to 1, 1L to Int.MAX_VALUE)
            for ((offset, length) in ranges) {
                assertThat(runCatching { source.read(offset, length) }.exceptionOrNull())
                    .isInstanceOf(IllegalArgumentException::class.java)
            }
        }
    }

    @Test
    fun `closing twice is safe and prevents subsequent reads`() {
        val source = FileRandomAccessStream(temporary.newFile().apply { writeText("data") })

        source.close()
        source.close()

        assertThat(runCatching { source.read(0, 1) }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `a truncated file raises an error instead of returning a short range`() {
        val file = temporary.newFile().apply { writeText("data") }
        FileRandomAccessStream(file).use { source ->
            RandomAccessFile(file, "rw").use { it.setLength(1) }

            assertThat(runCatching { source.read(0, 4) }.exceptionOrNull())
                .isInstanceOf(EOFException::class.java)
        }
    }

    @Test
    fun `interrupted readers fail before accessing the file`() {
        val file = temporary.newFile().apply { writeText("data") }
        FileRandomAccessStream(file).use { source ->
            try {
                Thread.currentThread().interrupt()
                assertThat(runCatching { source.read(0, 1) }.exceptionOrNull())
                    .isInstanceOf(InterruptedIOException::class.java)
            } finally {
                Thread.interrupted()
            }
            assertThat(source.read(0, 4)).isEqualTo("data".toByteArray())
        }
    }
}
