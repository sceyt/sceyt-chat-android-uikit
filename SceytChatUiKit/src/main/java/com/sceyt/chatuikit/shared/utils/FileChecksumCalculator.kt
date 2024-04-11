package com.sceyt.chatuikit.shared.utils

import com.sceyt.chatuikit.extensions.getFileSize
import java.io.File
import java.io.FileInputStream
import java.util.zip.CRC32


object FileChecksumCalculator {

    fun calculateFileChecksum(filePath: String): Long? {
        val length = getFileSize(filePath)
        if (length == 0L) return null

        val mb1 = 1L * 1024 * 1024
        val mb3 = 3L * 1024 * 1024

        var fis: FileInputStream? = null
        val checksum = try {
            fis = FileInputStream(File(filePath))
            val crc32 = CRC32()
            if (length < mb3) {
                calculateChecksumForFile(fis, crc32)
            } else {
                calculateChecksumFor1Mb(fis, crc32, 0)
                calculateChecksumFor1Mb(fis, crc32, length / 3)
                calculateChecksumFor1Mb(fis, crc32, length - mb1)
            }
            val result = crc32.value
            if (result == 0L) null else result
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } finally {
            fis?.close()
        }

        return checksum
    }

    private fun calculateChecksumFor1Mb(fis: FileInputStream, crc32: CRC32, skip: Long) {
        try {
            val loopBufferSize = 8192 * 4 //32 kb
            val maxBufferSize = 1024 * 1024 * 1 //1 mb
            var bytesRead = 0
            fis.skip(skip)
            var loadedBufferSize = 0
            val buffer = ByteArray(loopBufferSize)
            while (loadedBufferSize < maxBufferSize && fis.read(buffer).also { bytesRead = it } != -1) {
                loadedBufferSize += loopBufferSize
                crc32.update(buffer, 0, bytesRead)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateChecksumForFile(fis: FileInputStream, crc32: CRC32) {
        try {
            val loopBufferSize = 8192 * 4 //32 kb
            var bytesRead: Int
            val buffer = ByteArray(loopBufferSize)
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                crc32.update(buffer, 0, bytesRead)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
