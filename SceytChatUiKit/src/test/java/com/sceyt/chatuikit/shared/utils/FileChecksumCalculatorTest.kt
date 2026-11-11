package com.sceyt.chatuikit.shared.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.CRC32

class FileChecksumCalculatorTest {
    @get:Rule
    val files = TemporaryFolder()

    @Test
    fun `missing and empty files have no checksum`() {
        assertThat(FileChecksumCalculator.calculateFileChecksum(File(files.root, "missing").path)).isNull()
        assertThat(FileChecksumCalculator.calculateFileChecksum(files.newFile().path)).isNull()
    }

    @Test
    fun `small file checksum matches CRC32 of every byte`() {
        val bytes = ByteArray(100_001) { (it % 251).toByte() }
        val source = files.newFile().apply { writeBytes(bytes) }
        val expected = CRC32().apply { update(bytes) }.value

        assertThat(FileChecksumCalculator.calculateFileChecksum(source.path)).isEqualTo(expected)
    }

    @Test
    fun `identical bytes at different paths produce same checksum`() {
        val first = files.newFile().apply { writeText("same attachment") }
        val second = files.newFile().apply { writeBytes(first.readBytes()) }

        assertThat(FileChecksumCalculator.calculateFileChecksum(first.path))
            .isEqualTo(FileChecksumCalculator.calculateFileChecksum(second.path))
    }

    @Test
    fun `changed small file content changes checksum`() {
        val source = files.newFile().apply { writeText("first attachment") }
        val first = FileChecksumCalculator.calculateFileChecksum(source.path)
        source.writeText("other attachment")

        assertThat(FileChecksumCalculator.calculateFileChecksum(source.path)).isNotEqualTo(first)
    }

    @Test
    fun `large file checksum is repeatable and detects changes to first sample`() {
        val bytes = ByteArray(4 * 1024 * 1024) { (it % 251).toByte() }
        val source = files.newFile().apply { writeBytes(bytes) }
        val first = FileChecksumCalculator.calculateFileChecksum(source.path)
        assertThat(first).isNotNull()
        assertThat(FileChecksumCalculator.calculateFileChecksum(source.path)).isEqualTo(first)

        bytes[0] = 100
        source.writeBytes(bytes)

        assertThat(FileChecksumCalculator.calculateFileChecksum(source.path)).isNotEqualTo(first)
    }

    @Test
    fun `large file checksum matches samples at absolute offsets`() {
        val mb = 1024 * 1024
        for (length in listOf(3 * mb, 4 * mb, 4 * mb + 37)) {
            val bytes = ByteArray(length) { (it % 251).toByte() }
            val source = files.newFile().apply { writeBytes(bytes) }
            val expected = CRC32().apply {
                for (offset in listOf(0, length / 3, length - mb))
                    update(bytes, offset, mb)
            }.value

            assertThat(FileChecksumCalculator.calculateFileChecksum(source.path)).isEqualTo(expected)
        }
    }

    @Test
    fun `large file checksum must include the final sample`() {
        val bytes = ByteArray(4 * 1024 * 1024) { (it % 251).toByte() }
        val source = files.newFile().apply { writeBytes(bytes) }
        val first = FileChecksumCalculator.calculateFileChecksum(source.path)
        bytes[bytes.lastIndex] = (bytes.last() + 1).toByte()
        source.writeBytes(bytes)

        assertThat(FileChecksumCalculator.calculateFileChecksum(source.path)).isNotEqualTo(first)
    }
}
